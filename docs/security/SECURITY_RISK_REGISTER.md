# CIAP security risk register

Owners below are roles, not named people. Nothing here was accepted as a production risk by TIRDO; these are the items this audit could not close in code.

| ID | Risk | Severity | Mitigation now | Owner | Next step |
| --- | --- | --- | --- | --- | --- |
| CIAP-04 | Development JWT fallback and bootstrap password remain in Git history, and the local database may still contain the original seeded administrator. | High | Docker refuses those values on a new boot. Password is not logged. Change-password revokes sessions. | Platform operator | Rotate the live JWT secret. Change the seeded administrator password. Do not deploy with the fallback. |
| CIAP-13 | Finance or admin can record a payment as settled without a GePG signature. | Medium | Endpoint requires finance or admin. It is not anonymous. | Finance system owner | Specify GePG signature verification before exposing the endpoint to GePG. |
| CIAP-14 | MFA is a database flag only. | Medium | Passwords are BCrypt. Login is rate limited. | Security officer | Decide whether TIRDO requires MFA. If yes, design enrollment and recovery before coding it. |
| CIAP-15 | Tokens in localStorage are exposed to any future XSS. | Medium | No unsafe HTML rendering found. The page has no content security policy, because the one tried here broke the existing layout and fonts. | Application owner | Revisit HttpOnly cookies and CSRF if the threat model requires it. |
| CIAP-18 | MySQL connections inside Compose do not use TLS. | Medium | Database port is not published. Application user is not root. | Platform operator | Add certificates or accept plaintext on a private network in writing. |
| CIAP-21 | Dependency vulnerabilities may exist in Spring Boot 3.3.5, Angular 21, and base images. No advisory scan was completed. | Medium | Versions are pinned to the project's current releases, not floating `latest` except image tags such as `mysql:8.4`. | Platform operator | Run Trivy or OWASP Dependency-Check in CI and patch high findings that have a compatible release. |
| CIAP-22 | No pipeline, so there is no automated gate, pinned action, or protected production deploy. | Medium | Deployment is manual Compose. | Platform operator | Add a pipeline that runs tests and does not deploy unreviewed builds. |
| CIAP-12 | Registration still returns 409 when the email or TIN exists, which can be used to probe accounts. | Low | The response text no longer contains the value. | Application owner | Return one response for both new and existing registrations if product owners accept the UX change. |
| CIAP-23 | Database health check shows the root password in container inspect. | Low | The port is not published. | Platform operator | Switch the health check to a socket or a secrets-mounted config file. |
| CIAP-24 | Nginx master process runs as root. | Low | `no-new-privileges` is set. Backend runs as `ciap`. | Platform operator | Use an image that runs Nginx unprivileged if operations will support it. |
| Rate limit | The auth limiter is per process and resets on restart. | Low | It slows online guessing against one instance. | Platform operator | Move the limiter to a shared store if more than one backend replica is run. |
| Backups | Backup encryption, access, and restore are not implemented in this repo. | Medium | Volumes survive `docker compose down` without `-v`. | Platform operator | Document and test restore. Never use `-v` on a database you need to keep. |

## Verification still required

- HTTPS and HSTS on the real hostname.
- Firewall rules for any host that runs this Compose file.
- That the local seeded administrator password has been changed.
- A current CVE scan of dependencies and images.
- That expert user accounts are linked to consultant records before experts are expected to open assignments.
