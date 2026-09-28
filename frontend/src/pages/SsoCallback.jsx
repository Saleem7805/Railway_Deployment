import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { setToken, getToken } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function SsoCallback() {
  const { refresh, user } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState('')

  useEffect(() => {
    const raw = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : ''
    const params = new URLSearchParams(raw)
    const token = params.get('token')
    if (!token) {
      setError('SSO sign-in did not return an LMS session token.')
      return
    }
    setToken(token)
    window.history.replaceState({}, document.title, window.location.pathname)
    refresh().then(() => {}).catch(() => setError('SSO sign-in could not be completed.'))
  }, [refresh])

  useEffect(() => {
    if (user) {
      const home = { LEARNER: '/learn', MENTOR: '/mentor', ADMIN: '/admin', SUPER_ADMIN: '/super' }
      navigate(user.mustChangePassword ? '/set-password' : home[user.role] || '/', { replace: true })
    }
  }, [user, navigate])

  if (getToken() && !error && !user) return <div className="auth-wrap"><div className="auth-card"><h2>Signing you in…</h2><p className="text-muted">Completing your SSO session.</p></div></div>
  if (error) return <div className="auth-wrap"><div className="auth-card"><h2>SSO sign-in failed</h2><p className="text-muted">{error}</p><a className="btn btn-pib w-100" href="/">Return to sign in</a></div></div>
  return <div className="auth-wrap"><div className="auth-card"><h2>Signing you in…</h2></div></div>
}
