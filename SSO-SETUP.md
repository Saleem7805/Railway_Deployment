# LMS SSO setup (OIDC)

The LMS now supports provider-agnostic OpenID Connect SSO for all four existing roles:

- SUPER_ADMIN
- ADMIN
- MENTOR
- LEARNER

SSO does **not** create or change LMS roles. The external identity is matched to an existing LMS `User` by email. The role already stored on that LMS user controls access after SSO.

## 1. Configure the identity provider

Create an OIDC application/client in your identity provider (Keycloak, Microsoft Entra ID, Okta, Google Workspace, or another OIDC provider).

Use this callback URL in the provider:

`http://localhost:8080/login/oauth2/code/lms`

For production replace the host with the public LMS backend URL, for example:

`https://api.example.com/login/oauth2/code/lms`

The provider must return an `email` claim (or `preferred_username`/`upn`).

## 2. Backend environment variables

Set these before starting Spring Boot:

```text
SSO_ENABLED=true
SSO_CLIENT_ID=<your-oidc-client-id>
SSO_CLIENT_SECRET=<your-oidc-client-secret>
SSO_ISSUER_URI=https://<your-provider>/realms/<your-realm>
LMS_BASE_URL=http://localhost:5173
```

For Microsoft Entra ID, the issuer is the tenant's OIDC issuer URL. For Keycloak it is normally the realm issuer URL.

## 3. User provisioning

Each person must already exist in the LMS `users` collection with the same email as the SSO account.

The existing LMS role is retained:

```text
email                 role
--------------------  ----------------
sa@company.com        SUPER_ADMIN
admin@company.com     ADMIN
mentor@company.com    MENTOR
learner@company.com   LEARNER
```

An SSO user that is not provisioned receives a clear access-denied response instead of having an LMS account silently created.

## 4. Login flow

The learner login page and staff login page both contain **Continue with SSO**.

The flow is:

1. User selects **Continue with SSO**.
2. The browser is redirected to the configured OIDC provider.
3. The provider authenticates the user.
4. Spring Security validates the OIDC response.
5. LMS matches the returned email to an existing LMS user.
6. The normal LMS device/session rules are applied.
7. LMS issues its existing JWT.
8. The browser returns to `/sso/callback` and enters the normal LMS application.

The existing password login remains available.
