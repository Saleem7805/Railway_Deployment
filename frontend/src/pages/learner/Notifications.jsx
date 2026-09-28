import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { Card, Empty, Page, Tag, fmtDateTime } from '../../components/Ui'
import { TableSkeleton } from '../../components/Skeletons'

export default function Notifications() {
  const [items, setItems] = useState(null)

  const load = () => api.get('/learner/notifications').then(setItems)
  useEffect(() => {
    load()
    api.post('/learner/notifications/read', {}).catch(() => {})
  }, [])

  if (!items) return <TableSkeleton />

  return (
    <Page title="Notifications"
      lede="Task assignments, submission updates and mentor feedback.">
      <Card>
        {items.length === 0 ? <Empty title="No notifications yet" /> : (
          <div className="d-flex flex-column gap-3">
            {items.map((n) => (
              <div key={n.id} className="card-pib" style={{ marginBottom: 0 }}>
                <div className="d-flex align-items-center gap-2">
                  <strong>{n.subject}</strong>
                  {!n.read && <Tag kind="stop">New</Tag>}
                  <span className="small muted ms-auto">{fmtDateTime(n.sentAt)}</span>
                </div>
                <div className="small mt-2" style={{ whiteSpace: 'pre-wrap' }}>{n.body}</div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </Page>
  )
}
