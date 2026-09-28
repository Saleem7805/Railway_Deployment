import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { TableSkeleton } from '../../components/Skeletons'
import { Card, Empty, Page, Tag } from '../../components/Ui'

const fmt = (v) => v ? new Date(v).toLocaleString() : '—'
const mins = (v = 0) => `${Math.floor(v / 60)}h ${v % 60}m`

export default function StudyReport() {
  const [days, setDays] = useState(30)
  const [rows, setRows] = useState(null)
  const load = () => api.get(`/admin/study-report?days=${days}`).then(setRows)
  useEffect(() => { load() }, [days])

  if (!rows) return <TableSkeleton />

  return (
    <Page title="Check-in / Check-out report"
      lede="Admin view of learner and mentor attendance sessions.">
      <Card
        title="Attendance sessions"
        actions={
          <select className="form-select" style={{ width: 150 }} value={days}
            onChange={(e) => setDays(Number(e.target.value))}>
            {[7, 30, 60, 90].map((d) => <option key={d} value={d}>Last {d} days</option>)}
          </select>
        }
      >
        {rows.length === 0 ? (
          <Empty title="No check-in records" />
        ) : (
          <table className="table table-pib stacked mb-0">
            <thead>
              <tr><th>Name</th><th>Role</th><th>Check in</th><th>Check out</th><th>Duration</th><th>Status</th></tr>
            </thead>
            <tbody>
              {rows.map((r) => (
                <tr key={r.id}>
                  <td data-label="Name">
                    <strong>{r.name}</strong>
                    <div className="tiny muted">{r.email}</div>
                  </td>
                  <td data-label="Role">{r.role}</td>
                  <td data-label="Check in" className="mono">{fmt(r.checkedInAt)}</td>
                  <td data-label="Check out" className="mono">{fmt(r.checkedOutAt)}</td>
                  <td data-label="Duration" className="mono">{mins(r.minutes)}</td>
                  <td data-label="Status">
                    <Tag kind={r.status === 'CHECKED_IN' ? 'ok' : 'wait'}>
                      {r.status === 'CHECKED_IN' ? 'Checked in' : 'Checked out'}
                    </Tag>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </Page>
  )
}
