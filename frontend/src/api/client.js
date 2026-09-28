const BASE = import.meta.env.VITE_API_BASE || '/api'

let token = localStorage.getItem('pib.token') || null

/** A stable, meaningless id for this browser. It identifies a device, not a person. */
function deviceId() {
  let id = localStorage.getItem('pib.device')
  if (!id) {
    id = (crypto.randomUUID?.() || Math.random().toString(36).slice(2) + Date.now())
    localStorage.setItem('pib.device', id)
  }
  return id
}

export function deviceLabel() {
  const ua = navigator.userAgent
  const browser = /Edg/.test(ua) ? 'Edge' : /Chrome/.test(ua) ? 'Chrome'
    : /Safari/.test(ua) ? 'Safari' : /Firefox/.test(ua) ? 'Firefox' : 'Browser'
  const os = /Windows/.test(ua) ? 'Windows' : /Mac/.test(ua) ? 'Mac'
    : /Android/.test(ua) ? 'Android' : /iPhone|iPad/.test(ua) ? 'iOS' : 'Desktop'
  return `${browser} on ${os}`
}

export { deviceId }

export function setToken(t) {
  token = t
  if (t) localStorage.setItem('pib.token', t)
  else localStorage.removeItem('pib.token')
}

export function getToken() {
  return token
}

/*
 * CSRF protection. The server hands out an XSRF-TOKEN cookie and refuses any
 * POST, PUT, PATCH or DELETE that does not echo it back in the X-XSRF-TOKEN header.
 * A page on another site can make the browser send our cookies but cannot read
 * them, so it can never produce the header. Reads (GET) are not checked.
 */
const CSRF_COOKIE = 'XSRF-TOKEN'
const CSRF_HEADER = 'X-XSRF-TOKEN'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

function readCookie(name) {
  const match = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null
}

/**
 * The cookie is set on the first response from the API. A sign-in form can be the
 * very first thing a person does, so if there is no cookie yet, one cheap GET
 * fetches it before the POST goes out.
 */
async function ensureCsrfToken(force = false) {
  if (!force && readCookie(CSRF_COOKIE)) return
  await fetch(`${BASE}/health`, { credentials: 'same-origin' }).catch(() => {})
}

async function send(method, path, body, isForm) {
  const headers = { 'X-Device-Id': deviceId() }
  if (token) headers.Authorization = `Bearer ${token}`
  if (body && !isForm) headers['Content-Type'] = 'application/json'
  if (!SAFE_METHODS.has(method)) {
    const csrf = readCookie(CSRF_COOKIE)
    if (csrf) headers[CSRF_HEADER] = csrf
  }

  return fetch(`${BASE}${path}`, {
    method,
    headers,
    credentials: 'same-origin',
    body: isForm ? body : body ? JSON.stringify(body) : undefined
  })
}

async function request(method, path, body, isForm = false) {
  const unsafe = !SAFE_METHODS.has(method)
  if (unsafe) await ensureCsrfToken()

  let res = await send(method, path, body, isForm)

  /*
   * A 403 on a write can mean the CSRF token was rotated (it is replaced after an SSO
   * sign-in) or the cookie was cleared. A rejected request was never processed, so it
   * is safe to fetch a fresh token and try exactly once more. A genuine permission
   * problem simply comes back 403 a second time and is reported as before.
   */
  if (unsafe && res.status === 403) {
    await ensureCsrfToken(true)
    res = await send(method, path, body, isForm)
  }

  /*
   * A failed sign in is also a 401, and treating it as an ended session told people
   * their session had expired when they had simply mistyped a password. The login
   * endpoint is the one place where 401 means "those credentials are wrong", so its
   * own message is what should reach the screen.
   */
  if (res.status === 401 && !path.startsWith('/auth/login') && !path.startsWith('/auth/me')) {
    /*
     * A 401 used to sign the person out on the spot, from any request, including
     * background ones they never made. That is how a mentor opening a learner ended
     * up back at the sign-in screen: the record page also asks two admin-only
     * endpoints, and one refused answer tore down a session that was perfectly fine.
     *
     * The token is now checked before anything is thrown away. If the session still
     * answers, the 401 belonged to that one endpoint and the caller deals with it.
     * Only a session that has genuinely stopped working signs anybody out.
     */
    const replaced = res.headers.get('X-Session-Ended') === '1'

    if (!replaced && token) {
      const stillLive = await fetch(`${BASE}/auth/me`, {
        headers: { Authorization: `Bearer ${token}`, 'X-Device-Id': deviceId() }
      }).then((r) => r.ok).catch(() => false)

      if (stillLive) {
        const err = new Error('You do not have access to that.')
        err.status = 401
        window.dispatchEvent(new CustomEvent('pib:request-failed', {
          detail: { path, method, message: err.message, status: 401 }
        }))
        throw err
      }
    }

    setToken(null)
    window.dispatchEvent(new CustomEvent('pib:signed-out', { detail: { replaced } }))
    throw new Error(replaced
      ? 'You were signed out because this account signed in somewhere else.'
      : 'Your session ended. Sign in again.')
  }

  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    /*
     * The server's 500 reply carries the real cause in `detail`, and the client used to
     * read only `error`, so every unexpected failure reached the screen as the same
     * seven words with nothing to act on. The detail is now part of the message.
     */
    const err = new Error(
      data?.detail ? `${data.error || 'That request did not go through.'} (${data.detail})`
        : (data?.error || 'That request did not go through.')
    )
    err.status = res.status
    /*
     * A page that loads its own data with .then and no .catch leaves its state null
     * forever, so the skeleton never resolves and nothing on screen says why. Several
     * pages were built that way. Each one is being given a real error state, and until
     * they all have one this makes the failure audible rather than silent.
     */
    window.dispatchEvent(new CustomEvent('pib:request-failed', {
      detail: { path, method, message: err.message, status: res.status }
    }))
    throw err
  }
  return data
}

/**
 * Downloads have to go through fetch like everything else: authentication is a
 * bearer header, so a plain <a href> reaches the API with no credentials and comes
 * back 401. This pulls the bytes, then hands the browser a blob to save.
 */
export async function downloadFile(id, filename) {
  const res = await fetch(`${BASE}/files/${id}`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Device-Id': deviceId() }
  })
  if (!res.ok) throw new Error('That file could not be downloaded.')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename || 'download'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

export const api = {
  get: (p) => request('GET', p),
  post: (p, b) => request('POST', p, b),
  put: (p, b) => request('PUT', p, b),
  del: (p) => request('DELETE', p),
  upload: (p, formData) => request('POST', p, formData, true)
}
