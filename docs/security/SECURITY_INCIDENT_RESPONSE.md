# CIAP incident response

Initial procedures for this application. They are not a substitute for TIRDO's organisational incident process. Do not paste passwords, tokens, or document contents into tickets or chat logs.

## Exposed secret or JWT signing key

1. Treat every existing access and refresh token as stolen. They are signed by that key.
2. Put a new `CIAP_JWT_SECRET` in the secret store and restart the backend. Old tokens fail signature checks.
3. Users must sign in again. Tell them through an out-of-band channel if the key was public.
4. If the exposed value is the development fallback that was committed in Git, assume it is known. Rotating the live secret is required even if Git history is not rewritten. Rewriting history is a separate decision and does not remove copies already cloned.
5. Check backend logs for unexpected administrator actions after the exposure window. Do not collect more personal data than the investigation needs.

## Compromised account

1. If the person can still sign in, they call `POST /auth/change-password` with the current password and a new one. That revokes every token for the account.
2. If they cannot sign in, an administrator disables the user (`users.enabled = false`) in the database and then increments `token_version` so existing tokens die. Do this from a reviewed change, not an ad-hoc production console session without a record.
3. There is no email reset and no MFA recovery flow. Do not invent a temporary password in a ticket.
4. Review consultancy requests, quotations, and documents touched by that account during the window.

## Confirmed vulnerability

1. Record the finding id, affected version, and whether it is already fixed in this tree (`SECURITY_AUDIT.md`).
2. If it is unfixed and exploitable on a shared environment, restrict network access to the affected endpoint before a code deploy.
3. Do not run exploit payloads against production to "confirm" impact.
4. Patch on staging, run `ObjectLevelAccessTest` and `PipelineIntegrationTest`, then deploy through the normal approval path.
5. If the flaw exposed records across organisations, identify the affected client ids from audit columns (`created_by`, `updated_by`) and notify the data owner.

## What this application already records

- Failed logins and rejected refresh or password changes: warning logs with the email, not the password.
- Logout and password change: info logs with the email.
- Unexpected exceptions: the client receives "An unexpected error occurred" without a stack trace. The server log still has the exception for operators.

Logs are container stdout today. They are not shipped to a restricted store and there is no retention policy in this repository. That is an operational gap.
