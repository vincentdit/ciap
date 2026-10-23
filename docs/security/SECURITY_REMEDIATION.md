# CIAP security remediation

Changes stay inside the existing Spring Boot and Angular design. Staff workflow for admin, manager, and finance is unchanged: they can still see every engagement. Clients and experts are now limited to records they are allowed to see.

## Authentication

- Anonymous access is limited to `POST /auth/login`, `POST /auth/refresh`, and `POST /clients/register`. `POST /auth/staff` requires an authenticated administrator. `POST /auth/logout` and `POST /auth/change-password` require the caller's access token.
- The API filter accepts only access tokens whose `ver` claim matches `users.token_version`, and only for enabled users.
- Login stores one refresh-token id. Refresh succeeds only when the token type, version, and id match. A new refresh token replaces the stored id, so the previous refresh token cannot be reused.
- Logout and password change increment `token_version` and clear the refresh id. That revokes access and refresh tokens for that account, including other browsers.
- Flyway `V8__session_version.sql` adds `token_version` and `refresh_token_id`. Existing rows keep version 0. Tokens issued before the migration do not carry `ver` and are rejected; users sign in again.
- Failed logins are logged with the email only. The bootstrap password is no longer logged.
- BCrypt cost is 12 for new hashes. Existing hashes still verify.
- Login, refresh, registration, and password change are limited to 20 attempts per minute per client address (1,000 in the test profile). Behind Nginx, the docker profile uses `X-Real-IP` only when it looks like an IP address. The limiter is in memory and is not shared across instances.
- The docker profile refuses a JWT secret that is missing, shorter than 32 bytes, or equal to the development fallback in `application.yml`. On an empty database it also refuses to create the administrator with the built-in development password.

## Authorization

`AccessGuard` is applied to consultancy reads and the client/expert actions that change them, and to quotations, documents, draft knowledge articles, and draft training events.

- A client submit ignores a supplied `clientId` and uses that user's own organisation.
- A client list returns only that organisation's requests. This also makes the client dashboard and request list work; they previously received 403.
- An expert list and expert execution or report actions require the assigned consultant's linked user. Institutional roles are not restricted by that rule.
- Another organisation's record is returned as not found, so the response does not confirm that the id exists.
- Unpublished knowledge and training are hidden from clients. Registration of an unpublished event is rejected.
- Staff screens in the Angular app use `staffGuard`. The API remains the enforcement point.

## Uploads and browser

- Allowed extensions: pdf, png, jpg, jpeg, gif, webp, txt, csv, and common Office/OpenDocument types. Executables, HTML, and SVG are rejected. Stored names stay random. The stored content type comes from the extension.
- Downloads use `Content-Disposition: attachment` with a stripped filename and `X-Content-Type-Options: nosniff`.
- Nginx sends frame denial, nosniff, and a referrer policy. A page content security policy was removed because it blocked the existing Google fonts and left the main stylesheet in print mode, which stripped the layout. HSTS is intentionally absent on this HTTP deployment.
- The API sends nosniff, frame denial, referrer `no-referrer`, and a strict content security policy for JSON responses.
- Login ignores a `returnUrl` that is not a single-slash application path.
- Logout calls `POST /auth/logout` before dropping local tokens.

## Deployment configuration

- The committed Compose file no longer publishes the backend port. The browser uses port 8088. This workspace's untracked `docker-compose.override.yml` still publishes 8081 because host port 8080 is taken by another container.
- Containers set `no-new-privileges`.
- CORS origins come from `CIAP_CORS_ORIGINS` (default `http://localhost:4200,http://localhost:8088`).
- Bootstrap email and password environment variables are wired to `CIAP_BOOTSTRAP_ADMIN_EMAIL` and `CIAP_BOOTSTRAP_ADMIN_PASSWORD`.
- Example env files contain placeholders only.

## Behaviour that operators will notice

- Users with tokens issued before this change must sign in again.
- A client can no longer open another organisation's request, quotation, or document.
- An expert who is not linked to the assigned consultant record cannot open that engagement. Link the consultant to the expert's user account.
- A fresh Docker database will not start until `CIAP_JWT_SECRET` is unique and `CIAP_ADMIN_PASSWORD` is not the built-in development password.
- The administrator already stored in the local MySQL volume was not modified. Change that password with `POST /auth/change-password` after signing in. There is no in-app screen for it yet.
