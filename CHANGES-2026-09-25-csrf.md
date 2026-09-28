# CSRF protection (2026-09-25)

## What changed

| File | Change |
|---|---|
| `backend/.../security/SecurityConfig.java` | CSRF enabled (was `csrf.disable()`). Token in `XSRF-TOKEN` cookie (SameSite=Strict), checked against the `X-XSRF-TOKEN` header on every POST/PUT/PATCH/DELETE. `/login/oauth2/**` exempt (protected by the OAuth2 `state` parameter). Switch: `lms.csrf.enabled` / `CSRF_ENABLED`. |
| `backend/.../security/SpaCsrfTokenRequestHandler.java` | New. SPA token handler for Spring Security 6.3 (built in as `csrf.spa()` from 6.4). |
| `backend/.../security/SsoSuccessHandler.java` | After SSO issues the JWT, the leftover authenticated HTTP session is invalidated, so no cookie-based way in remains. |
| `backend/.../security/SsoOAuthClientConfig.java` | Reads `lms.sso.issuer-uri` / `client-id` / `client-secret`, falling back to the `SSO_*` environment variables (fixes "Could not resolve placeholder 'SSO_ISSUER_URI'"). |
| `backend/src/main/resources/application.yml` | Added `lms.csrf.enabled: ${CSRF_ENABLED:true}`. |
| `frontend/src/api/client.js` | Sends `X-XSRF-TOKEN` on every write, fetches the cookie first if missing, retries once on 403 with a fresh token. |

## How to test

1. Start backend and frontend, open the app, DevTools -> Application -> Cookies: `XSRF-TOKEN` is present.
2. Sign in, create/edit anything: works; the request shows the `X-XSRF-TOKEN` header.
3. Postman: `POST http://localhost:8080/api/auth/login` without the header -> **403**. Protection works.

## Deployment note

The frontend must reach the API on the same site (the Vite `/api` proxy in dev, a reverse
proxy in production) so the page can read the `XSRF-TOKEN` cookie. If `VITE_API_BASE`
points to a different domain, the header cannot be sent and writes will get 403.
