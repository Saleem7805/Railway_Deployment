# Certificate of completion (2026-09-25)

Admin / Super admin menu: **Learners -> Certificate of completion** (`/admin/certificate`).

1. Type the learner's **Name** and the **Course**. The preview updates as you type.
2. Press **Print**. In the browser dialog choose **Save as PDF** (or *Microsoft Print to PDF*) to get the PDF, or a printer to print it.
3. Keep **Background graphics** on so the border prints in colour. The page is A4 landscape with no margins, set automatically.
4. **Start the next certificate** clears the form and gives a new certificate number.

The date of issue and a certificate number (`PIB/<year>/<MMDD>-<nnnn>`) are filled in automatically.
The three signature lines are left blank for hand signing.

## Files
| File | Change |
|---|---|
| `frontend/src/pages/admin/Certificate.jsx` | New page: form, live preview, print. |
| `frontend/src/styles/certificate.css` | New: certificate design (mm, A4 landscape) and print rules. Only the certificate prints. |
| `frontend/src/App.jsx` | Route `/admin/certificate` (ADMIN, SUPER_ADMIN). |
| `frontend/src/components/AppShell.jsx`, `MobileNav.jsx`, `CommandPalette.jsx` | Menu entries. |
| `frontend/src/components/Icon.jsx` | `certificate` icon. |

No backend change. Certificates are not stored; the admin keeps the PDF.
