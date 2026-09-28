import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { Card, Page } from '../../components/Ui'
import '../../styles/certificate.css'

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July',
  'August', 'September', 'October', 'November', 'December']

/* A4 landscape is 297mm wide; browsers draw 1mm as 96/25.4 CSS pixels */
const SHEET_WIDTH_PX = 297 * (96 / 25.4)

function ordinalSuffix(n) {
  if (n % 100 >= 11 && n % 100 <= 13) return 'th'
  return { 1: 'st', 2: 'nd', 3: 'rd' }[n % 10] || 'th'
}

function shortDate(d) {
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`
}

/** A readable reference printed on the certificate, e.g. PIB/2026/0925-4821 */
function newCertificateNo(d = new Date()) {
  const mmdd = `${String(d.getMonth() + 1).padStart(2, '0')}${String(d.getDate()).padStart(2, '0')}`
  const tail = String(Math.floor(1000 + Math.random() * 9000))
  return `PIB/${d.getFullYear()}/${mmdd}-${tail}`
}

/**
 * The certificate itself. Drawn once in millimetres; the preview scales it and the
 * print copy uses it as is, so the PDF always matches what the admin looked at.
 */
export function CertificateSheet({ name, course, certificateNo, issuedOn }) {
  const day = issuedOn.getDate()
  return (
    <div className="cert-sheet">
      <div className="cert-frame">
        <div className="cert-inner">
          <div className="cert-watermark" aria-hidden="true">PROITBRIDGE</div>

          <div className="cert-top">
            <span>{shortDate(issuedOn)}</span>
            <span>No.: {certificateNo}</span>
          </div>

          <img className="cert-logo" src="/logo-ink.png" alt="ProITBridge" />

          <div className="cert-org">PROITBRIDGE</div>
          <div className="cert-sub">CENTER FOR ONLINE LEARNING</div>
          <div className="cert-title">Certificate of Completion</div>

          <div className="cert-small">hereby makes known that</div>
          <div className={name ? 'cert-name' : 'cert-name cert-blank'}>{name || 'Name'}</div>

          <div className="cert-small">has successfully completed the prescribed course of study in</div>
          <div className={course ? 'cert-course' : 'cert-course cert-blank'}>{course || 'Course'}</div>

          <div className="cert-small">and is hereby awarded this certificate of completion.</div>
          <div className="cert-issued">
            Issued this {day}<sup>{ordinalSuffix(day)}</sup> day of {MONTHS[issuedOn.getMonth()]} {issuedOn.getFullYear()}
          </div>

          <div className="cert-signs">
            <div className="cert-sign"><div className="cert-sign-line" />Program Director</div>
            <div className="cert-sign"><div className="cert-sign-line" />Principal</div>
            <div className="cert-sign"><div className="cert-sign-line" />Chairman</div>
          </div>
        </div>
      </div>
    </div>
  )
}

/**
 * Admin screen: type the learner's name and the course, check the preview, press
 * Print. The browser's print dialog saves it as a PDF ("Save as PDF" / "Microsoft
 * Print to PDF") or sends it to a printer. Only the certificate is printed, on one
 * A4 landscape page with no margins.
 */
export default function Certificate() {
  const [name, setName] = useState('')
  const [course, setCourse] = useState('')
  const [certificateNo, setCertificateNo] = useState(() => newCertificateNo())
  const [issuedOn] = useState(() => new Date())
  const [scale, setScale] = useState(0.6)
  const previewRef = useRef(null)

  const cleanName = name.trim()
  const cleanCourse = course.trim()
  const ready = cleanName.length > 0 && cleanCourse.length > 0

  /* keep the preview exactly as wide as its card, whatever the screen size */
  useLayoutEffect(() => {
    const el = previewRef.current
    if (!el) return undefined
    const fit = () => setScale(el.clientWidth / SHEET_WIDTH_PX)
    fit()
    const ro = new ResizeObserver(fit)
    ro.observe(el)
    return () => ro.disconnect()
  }, [])

  /* if the admin leaves mid-print, never leave the app stuck in print mode */
  useEffect(() => () => document.body.classList.remove('cert-printing'), [])

  function print() {
    if (!ready) return
    const previousTitle = document.title
    /* the browser offers the page title as the PDF file name */
    document.title = `Certificate - ${cleanName} - ${cleanCourse}`
    document.body.classList.add('cert-printing')

    const done = () => {
      document.body.classList.remove('cert-printing')
      document.title = previousTitle
      window.removeEventListener('afterprint', done)
    }
    window.addEventListener('afterprint', done)
    /* let the logo finish loading so it is never missing from the PDF */
    requestAnimationFrame(() => window.print())
  }

  function startNext() {
    setName('')
    setCourse('')
    setCertificateNo(newCertificateNo())
  }

  const sheet = (
    <CertificateSheet name={cleanName} course={cleanCourse}
      certificateNo={certificateNo} issuedOn={issuedOn} />
  )

  return (
    <Page title="Certificate of completion"
      lede="Type the learner's name and the course, check the preview, then print or save it as a PDF.">
      <Card title="Details">
        <form className="row g-3 align-items-end" onSubmit={(e) => { e.preventDefault(); print() }}>
          <div className="col-md-5">
            <label className="form-label" htmlFor="cert-name">Name</label>
            <input id="cert-name" className="form-control" value={name} maxLength={80}
              placeholder="e.g. Swathi Alamelu N" autoFocus
              onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="col-md-5">
            <label className="form-label" htmlFor="cert-course">Course</label>
            <input id="cert-course" className="form-control" value={course} maxLength={90}
              placeholder="e.g. Full Stack Java Development"
              onChange={(e) => setCourse(e.target.value)} />
          </div>
          <div className="col-md-2 d-grid">
            <button type="submit" className="btn btn-primary" disabled={!ready}
              title={ready ? 'Print, or choose "Save as PDF" in the dialog' : 'Enter the name and the course first'}>
              Print
            </button>
          </div>
        </form>
        <div className="tiny muted mt-2">
          In the print dialog choose <b>Save as PDF</b> (or <b>Microsoft Print to PDF</b>) to get the PDF.
          Keep <b>Background graphics</b> switched on so the border prints in colour.
          {' '}Certificate no. {certificateNo}.{' '}
          <button type="button" className="btn btn-link btn-sm p-0 align-baseline" onClick={startNext}>
            Start the next certificate
          </button>
        </div>
      </Card>

      <Card title="Preview">
        <div className="cert-preview" ref={previewRef}
          style={{ height: `${Math.round(SHEET_WIDTH_PX * (210 / 297) * scale)}px` }}>
          <div className="cert-preview-scale" style={{ transform: `scale(${scale})` }}>
            {sheet}
          </div>
        </div>
      </Card>

      {/* the copy that is printed: a direct child of <body>, shown only while printing */}
      {createPortal(<div className="cert-print-root">{sheet}</div>, document.body)}
    </Page>
  )
}
