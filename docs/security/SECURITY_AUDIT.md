# CIAP security audit

Audit date: 2026-10-23. Scope is the repository at `ciap/` as it exists in this workspace: Spring Boot 3.3.5 API (`ciap-backend`), Angular frontend (`ciap-frontend`), and the Docker Compose stack (MySQL 8.4, backend, Nginx). No CI/CD workflows are present. No mobile client is present.

This is not a statement that CIAP is secure. Passing the tests below does not cover production network policy, TLS certificates, backup restoration, or a live dependency advisory database.

## System

CIAP is a web application and JSON API for TIRDO consultancy work. The browser talks only to Nginx on port 8088. Nginx serves the Angular bundle and proxies `/api/` to the backend. The backend uses stateless JWTs (access and refresh), BCrypt password hashes, and role checks (`ADMIN`, `MANAGER`, `FINANCE`, `EXPERT`, `CLIENT`). Flyway owns the MySQL schema. Uploaded files are stored on a volume outside the web root.

Sensitive data present in the model: organisation identity and TIN, contact details, consultancy scope, quotations and contract values, payment control numbers, and uploaded documents. GePG is a staff-entered settlement action, not a signed external callback.

## Trust boundaries

- Anonymous callers may only log in, refresh a token, or register a client organisation.
- A client organisation may see and act only on its own engagements.
- An expert may see and update only engagements assigned to that expert's linked user account.
- Admin, manager, and finance keep organisation-wide access. That is the existing staff workflow.
- MySQL is not published on the host. The backend port is no longer published by the committed Compose file.

## Findings

| ID | Severity | Confidence | Status | Issue |
| --- | --- | --- | --- | --- |
| CIAP-01 | High | Confirmed | Fixed | Any client could read, cancel, quote-accept, or download another organisation's engagement by numeric id. `GET /consultancy-requests/{id}`, quotation accept, and document download checked the role only. |
| CIAP-02 | High | Confirmed | Fixed | `JwtAuthenticationFilter` accepted a refresh token as an API credential, so a 7-day refresh token worked as a bearer token. |
| CIAP-03 | High | Confirmed | Fixed | Logout only cleared the browser. Refresh "rotation" issued a new token and left the old one valid. |
| CIAP-04 | High | Confirmed | Mitigated | The development JWT fallback and bootstrap password are in source. Startup used to log the new administrator password. Docker now refuses that JWT value and refuses to seed that password into an empty database. An already-seeded local database is unchanged. Git history still contains the old values. |
| CIAP-05 | Medium | Confirmed | Fixed in docker | Swagger and OpenAPI were anonymous. The docker profile disables them. A local non-docker run can still expose them when `ciap.security.expose-api-docs` is true. |
| CIAP-06 | Medium | Confirmed | Fixed | Login, refresh, registration, and password change had no rate limit. |
| CIAP-07 | Medium | Confirmed | Fixed | Uploads trusted the client filename and content type, and the download header reflected the original filename. |
| CIAP-08 | Medium | Confirmed | Fixed | Any authenticated user, including a client, could read unpublished knowledge articles and training events by id, and could register for an unpublished event. |
| CIAP-09 | Medium | Confirmed | Fixed | `returnUrl` after login was passed to `navigateByUrl` without checking that it was a same-app path. |
| CIAP-10 | Medium | Confirmed | Partial | Nginx sends clickjacking, content-type, and referrer headers. The page content security policy was removed because it blocked the existing fonts and left the stylesheet in print mode. The API still sends a content security policy. HSTS is not set: this stack is served over HTTP. |
| CIAP-11 | Medium | Confirmed | Fixed | Clients were forbidden from listing requests, so their own dashboard could not load, while fetch-by-id was open. Listing is now allowed and filtered to the caller. |
| CIAP-12 | Low | Confirmed | Mitigated | Public registration returned a message that included the email or TIN. The message is now generic. HTTP 409 still differs from success. |
| CIAP-13 | Medium | Confirmed | Open | `POST /payments/gepg-callback` lets finance or admin mark a payment paid. The staff UI calls it. There is no GePG signature. A shared secret in the Angular bundle would not be a secret. |
| CIAP-14 | Medium | Confirmed | Open | `users.mfa_enabled` is stored and never checked. There is no enrollment or second factor. |
| CIAP-15 | Medium | Confirmed | Open | Access and refresh tokens are in `localStorage`. No `innerHTML` or `bypassSecurityTrust` usage was found. A future XSS would be able to read the tokens. Moving to HttpOnly cookies would require CSRF protection and is an architecture change. |
| CIAP-16 | Low | Confirmed | Fixed | There was no way for a signed-in user to replace their password. `POST /auth/change-password` now does that and revokes existing tokens. There is still no email recovery flow. |
| CIAP-17 | Low | Confirmed | Fixed in Compose | The committed Compose file published the API on host port 8080. That mapping is removed. This workspace's untracked override still publishes 8081 for local debugging, and port 8080 on this machine is already used by another stack. |
| CIAP-18 | Medium | Confirmed | Open | JDBC uses `useSSL=false` on the Compose network. MySQL is not published, but the connection is not encrypted. Enabling TLS needs certificates that this stack does not have. |
| CIAP-19 | Low | Confirmed | Fixed | Page requests were not capped. Maximum page size is 100. |
| CIAP-20 | Low | Confirmed | Fixed | `server.error.include-message` was `always`. It is now `never`. The API's own error contract still returns a short message and does not return a stack trace. |
| CIAP-21 | Info | Unverified | Open | Maven and npm advisory scanning against a vulnerability database was not completed. The host has no Java or Node. Tests ran in a Maven container. See the test report. |
| CIAP-22 | Info | Confirmed | Open | No GitHub Actions, GitLab CI, or Jenkins files exist, so pipeline hardening was not applicable. |
| CIAP-23 | Low | Confirmed | Open | The MySQL health check passes the root password on the command line, which is visible via `docker inspect`. |
| CIAP-24 | Low | Confirmed | Open | The Nginx image still starts its master process as root, which is the image default. The backend container already runs as the non-root `ciap` user. |

## Evidence locations

- Authorization gap: `ConsultancyRequestController`, `QuotationController`, `DocumentController`, and the matching services before ownership checks.
- Token acceptance: `JwtAuthenticationFilter` and `JwtService.isValid` previously ignored the `type` claim.
- Password logging: `DataInitializer` previously logged the bootstrap password.
- Public API docs: `SecurityConfig` previously permitted `/auth/**`, Swagger, and OpenAPI.
- Upload path: `DocumentService` and `DocumentController.download`.
- Frontend redirect: `login.component.ts`.

## What was not tested

No production host, firewall, DNS, TLS certificate, backup restore, or external GePG endpoint was available. Dynamic testing was limited to the local H2 test profile and, after rebuild, the local Docker stack. No destructive tests were run against the MySQL volume.
