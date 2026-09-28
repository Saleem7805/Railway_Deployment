import { useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import { useToast } from '../context/ToastContext'

function hhmm(minutes = 0) {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return h ? `${h}h ${m}m` : `${m}m`
}

export default function StaffCheckIn() {
  const [state, setState] = useState(null)
  const timer = useRef(null)
  const active = useRef(true)
  const toast = useToast()

  const load = async () => {
    try { setState(await api.get('/staff/study')) } catch { /* not available during sign-out */ }
  }

  useEffect(() => { load() }, [])

  useEffect(() => {
    const seen = () => { active.current = true }
    const gone = () => { active.current = document.visibilityState === 'visible' }
    document.addEventListener('visibilitychange', gone)
    window.addEventListener('mousemove', seen, { passive: true })
    window.addEventListener('keydown', seen)
    return () => {
      document.removeEventListener('visibilitychange', gone)
      window.removeEventListener('mousemove', seen)
      window.removeEventListener('keydown', seen)
    }
  }, [])

  useEffect(() => {
    if (!state?.checkedIn) { clearInterval(timer.current); return }
    timer.current = setInterval(async () => {
      if (!active.current || document.visibilityState !== 'visible') return
      active.current = false
      try {
        const r = await api.post('/staff/study/heartbeat', {})
        setState((s) => s ? { ...s, liveMinutes: r.minutes, todayMinutes: r.minutes } : s)
      } catch {}
    }, 120000)
    return () => clearInterval(timer.current)
  }, [state?.checkedIn])

  if (!state) return null

  const toggle = async () => {
    try {
      if (state.checkedIn) {
        const r = await api.post('/staff/study/check-out', {})
        setState(r)
        toast.push(`Checked out. ${hhmm(r.todayMinutes)} today.`)
      } else {
        const r = await api.post('/staff/study/check-in', {})
        setState(r)
        toast.push('Checked in. Your working time is counting.')
      }
    } catch (e) { toast.push(e.message, 'bad') }
  }

  return (
    <div className="checkin">
      <button
        className={`checkin-btn ${state.checkedIn ? 'on' : ''}`}
        onClick={toggle}
        title={state.checkedIn ? 'Check out and settle your time' : 'Start counting your work time'}
      >
        <span className="checkin-dot" />
        {state.checkedIn ? 'Checked in' : 'Check in'}
      </button>
      <span className="checkin-time mono">{hhmm(state.todayMinutes || 0)}</span>
    </div>
  )
}
