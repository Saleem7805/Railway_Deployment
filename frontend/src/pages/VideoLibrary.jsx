import { useEffect, useState } from 'react'
import { api } from '../api/client'
import SecurePlayer from '../components/SecurePlayer'
import Drawer from '../components/Drawer'
import { Card, Empty, Page, Tag } from '../components/Ui'
import { TableSkeleton } from '../components/Skeletons'

export default function VideoLibrary() {
  const [videos, setVideos] = useState(null)
  const [open, setOpen] = useState(null)

  useEffect(() => {
    api.get('/videos').then(setVideos)
  }, [])

  if (!videos) return <TableSkeleton />

  return (
    <Page title="Video library"
      lede="Videos added by Super Admin and available to the LMS team and learners.">
      <div className="grid g2">
        {videos.filter(v => v.active).map((v) => (
          <Card key={v.id} title={v.title}
            note={`${v.provider}${v.durationSec ? ` · ${Math.round(v.durationSec / 60)} min` : ''}`}
            actions={<Tag kind="ok">Available</Tag>}>
            <button className="btn btn-pib" onClick={() => setOpen(v)}>Watch video</button>
          </Card>
        ))}
      </div>
      {videos.filter(v => v.active).length === 0 && <Empty title="No videos available yet" />}
      <Drawer
        open={Boolean(open)}
        title={open?.title || 'Video'}
        subtitle="Shared LMS video"
        onClose={() => setOpen(null)}
      >
        {open && (
          <SecurePlayer videoRef={open.id} poster={open.title} track={false}
            onEnded={() => {}} />
        )}
      </Drawer>
    </Page>
  )
}
