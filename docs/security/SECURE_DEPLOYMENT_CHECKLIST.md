# CIAP secure deployment checklist

Do not deploy this change to production from this workspace. The steps below are the conditions a production rollout still has to meet. No DNS, firewall, certificate, or production credential change was made here.

## Before rollout

- Take a MySQL backup and confirm it restores on a spare instance. This change adds Flyway `V8__session_version.sql` only. It does not drop or rewrite business data. Tokens issued before V8 stop working and users must sign in again.
- Set `CIAP_JWT_SECRET` to a new base64 value of at least 32 bytes (`openssl rand -base64 48`). Do not reuse the development fallback that is still in `application.yml` and in Git history.
- Set `CIAP_ADMIN_PASSWORD` before the first boot of an empty database. The docker profile will not create the administrator with the built-in development password. On a database that already has users, change the existing password through `POST /auth/change-password` while signed in. This workspace did not rotate the already-seeded local account.
- Set `CIAP_CORS_ORIGINS` to the real HTTPS origin. Do not use `*`.
- Put secrets in the deployment secret store, not in Git. `.env` is gitignored. Keep `.env.example` as placeholders.
- Serve the site with HTTPS in front of Nginx. Add HSTS only after HTTPS works for that hostname. This Compose file listens on HTTP port 8088 and does not terminate TLS.
- Do not publish MySQL or the backend port. The browser should reach only the reverse proxy. Remove any local Compose override that publishes 8081 before a shared deployment.
- Plan a GePG signing design before treating `POST /payments/gepg-callback` as an external webhook. Today it is a finance/admin action.

## Rollout

1. Deploy to a staging environment that uses its own database and secrets.
2. Run the backend test suite in CI once a pipeline exists. Local result: see `SECURITY_TEST_REPORT.md`.
3. Confirm Flyway applies V8 and the backend becomes healthy.
4. Sign in as a client and confirm that organisation sees only its own requests.
5. Sign in as finance or admin and confirm the existing queue, quotation, and payment screens still load.
6. Watch logs for failed logins. Logs must not contain passwords, `Authorization` headers, or refresh tokens.

## Rollback

- Application rollback: redeploy the previous images. V8 columns are nullable or defaulted and can stay in place; the previous application will not know `token_version` and may fail Hibernate validation (`ddl-auto: validate`). If you must run the previous build, restore the database backup taken before V8 rather than dropping columns on a live system without a reviewed script.
- Do not use `docker compose down -v` as a rollback. That deletes the database and uploaded documents.

## Still required outside this repository

- TLS certificates and HTTPS.
- Network policy so MySQL and the backend are reachable only from the application.
- Encrypted database connections, or an explicit acceptance of plaintext on a private network.
- Backup encryption, access control, and a tested restore.
- Central logging and alerts for repeated failed logins and administrator actions.
- A vulnerability scan of Maven and npm dependencies against a current advisory database, then patch review.
- MFA, if TIRDO policy requires it. The column exists; the feature does not.
