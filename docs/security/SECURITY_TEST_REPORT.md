# CIAP security test report

Date: 2026-10-23. Host toolchain: Java, Maven, and Node are not installed. Backend tests ran in `maven:3.9-eclipse-temurin-21` with the backend directory mounted at `/build`.

## Baseline

A baseline run of the original commit was attempted with `git archive` into a temp directory. Windows `tar` rejected the archive (`Damaged tar archive`), so Maven found no `pom.xml` and did not execute the original tests. The pre-change suite result is therefore **not available**. The post-change suite includes the original test classes and they passed.

## Post-change suite

First full run, before the password-change endpoint:

| Class | Tests | Failures | Errors |
| --- | --- | --- | --- |
| RequestStatusTest | 6 | 0 | 0 |
| ConsultantWorkloadTest | 2 | 0 | 0 |
| ObjectLevelAccessTest | 3 | 0 | 0 |
| PipelineIntegrationTest | 4 | 0 | 0 |
| SecurityAccessTest | 6 | 0 | 0 |
| AttemptLimiterTest | 1 | 0 | 0 |
| **Total** | **22** | **0** | **0** |

Result: **BUILD SUCCESS**.

After `POST /auth/change-password` was added, these classes were run again: `ObjectLevelAccessTest` (3), `PipelineIntegrationTest` (4), `SecurityAccessTest` (6), `AttemptLimiterTest` (1). **14 tests, 0 failures, BUILD SUCCESS.** The password-change checks live inside `draftsAndStaffProvisioningStayClosed`. `RequestStatusTest` and `ConsultantWorkloadTest` were not re-run; they do not call authentication and passed in the full run above.

`ObjectLevelAccessTest` covers:

- A second client receives 404 for another organisation's request, cancel, quotation, and document download.
- That client's list does not contain the other organisation's request.
- A submit that names another `clientId` is stored against the caller's own organisation.
- An executable upload is rejected.
- A refresh token is rejected on `/clients/me`, while the access token is accepted.
- Logout then rejects that access token and the previous refresh token.
- A client receives 404 for an unpublished article and cannot register for an unpublished training event. The test administrator can read the draft.
- Anonymous `POST /auth/staff` is rejected.

`SecurityAccessTest` still checks that login is public, unauthenticated reads are rejected, a client cannot list every organisation, an admin can, an expert cannot read reports, and finance can.

`PipelineIntegrationTest` still walks the staff revenue path with no HTTP caller, which remains allowed for in-process workflow code.

## Not run

- Frontend unit tests. `package.json` has no test script and Node is not installed on the host.
- `npm audit` and an OWASP/Trivy advisory scan. Not executed in this session.
- A full browser sign-in. After the image rebuild, `GET /` returned 200 with `Content-Security-Policy`, `X-Frame-Options: DENY`, and `X-Content-Type-Options: nosniff`. `POST /api/auth/login` through Nginx returned 200. Unauthenticated `GET /api/clients` and `GET /api/swagger-ui.html` returned 403. The login page was opened and rendered. The password was not submitted in the browser.
- TLS, backup restore, and production network checks. No such environment was in scope.
- Load or denial-of-service testing. Not performed. The rate limit was checked with a fixed-window unit test, not by flooding the running server.

A passing test run does not prove the absence of further vulnerabilities.
