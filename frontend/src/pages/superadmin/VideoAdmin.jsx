import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import { useToast } from '../../context/ToastContext'
import { useDialog } from '../../components/Dialog'
import VideoField from '../../components/VideoField'
import { Card, Empty, Page, Tag } from '../../components/Ui'
import Drawer from '../../components/Drawer'
import SecurePlayer from '../../components/SecurePlayer'

export default function VideoAdmin() {
  const toast = useToast()
  const { ask } = useDialog()
  const [videos, setVideos] = useState([])
  const [open, setOpen] = useState(null)

  const load = () => api.get('/super/videos').then(setVideos)
  useEffect(() => { load() }, [])

  const add = async () => {
    const r = await ask({
      title: 'Add a video',
      body: 'This video is placed in the shared library. Super Admin adds it; Super Admin, Admin, Mentor and Learner can view it.',
      fields: [
        { name: 'title', label: 'Title', required: true, placeholder: 'Introduction to Python' },
        { name: 'externalId', label: 'YouTube link', required: true,
          placeholder: 'https://youtu.be/dQw4w9WgXcQ' }
      ],
      confirmLabel: 'Add video'
    })
    if (!r) return
    try {
      await api.post('/super/videos', { title: r.title.trim(), externalId: r.externalId, provider: 'YOUTUBE' })
      toast.push('Video added to the shared library.')
      load()
    } catch (e) { toast.push(e.message, 'bad') }
  }

  return (
    <Page title="Video library"
      lede="Add videos centrally. Every LMS role can view the shared library."
      actions={<button className="btn btn-pib" onClick={add}>Add video</button>}>
      <Card>
        {videos.length === 0 ? <Empty title="No shared videos yet" /> : (
          <table className="table table-pib mb-0">
            <thead><tr><th>Title</th><th>Provider</th><th>Duration</th><th>Status</th><th /></tr></thead>
            <tbody>
              {videos.map((v) => (
                <tr key={v.id}>
                  <td><strong>{v.title}</strong></td>
                  <td>{v.provider}</td>
                  <td className="mono">{v.durationSec ? `${Math.round(v.durationSec / 60)} min` : '—'}</td>
                  <td><Tag kind={v.active ? 'ok' : 'wait'}>{v.active ? 'Live' : 'Inactive'}</Tag></td><td className="text-end">{v.active && <button className="btn btn-quiet" onClick={() => setOpen(v)}>Watch</button>}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
      <Drawer open={Boolean(open)} title={open?.title || 'Video'} subtitle="Shared video library" onClose={() => setOpen(null)}>{open && <SecurePlayer videoRef={open.id} poster={open.title} track={false} onEnded={() => {}} />}</Drawer>
    </Page>
  )
}
