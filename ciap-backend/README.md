# CIAP Backend — Consultancy & Industrial Advisory Portal (TIRDO)

Revenue-generating industrial consultancy management platform.
**Stack:** Java 21 · Spring Boot 3.3 · Spring Security (JWT) · JPA/Hibernate · Flyway · MySQL 8.

This repository implements **Phase 1** of the CIAP blueprint — the modules the blueprint
recommends building first because they begin generating revenue within the first operational year:

| Blueprint module | Status in this repo |
|------------------|---------------------|
| 1. Authentication & Access Control (JWT, RBAC) | ✅ Implemented |
| 2. Client Relationship Management (CRM)         | ✅ Implemented |
| 3. Consultancy Request Management (workflow)    | ✅ Implemented |
| 4. Expert Management (skills, workload)         | ✅ Implemented |
| 5. Quotation & Contract Management              | ✅ Implemented (line items, contract on accept) |
| 7/8. Billing & GePG Integration                 | ✅ Control number + GePG callback driving the workflow |
| 6. Project Management (tasks, milestones, progress) | ✅ Implemented |
| 9–12. Knowledge, Training, Document, BI         | ⏳ Roadmap |

## Architecture

```
Angular (4200) ──HTTP──> Spring Boot REST (/api) ──JPA──> MySQL 8
                              │
                              ├─ Spring Security + JWT filter (stateless)
                              ├─ Flyway (owns the schema)
                              └─ Layered: controller → service → repository → entity
```

Package layout (`tz.go.tirdo.ciap`):

```
config/      SecurityConfig, OpenApiConfig, AuditingConfig, DataInitializer
security/    JwtService, JwtAuthenticationFilter, CustomUserDetailsService
common/      audit (Auditable), dto (ApiResponse, PageResponse), exception (global handler)
domain/
  user/        User, Role, Auth (login + staff provisioning)
  client/       Client (CRM + public self-registration)
  consultant/   Consultant (expert mgmt + workload engine)
  consultancy/  ConsultancyRequest + enforced workflow state machine
```

## Running locally

```bash
# 1. Start MySQL
docker compose up -d

# 2. Run the app (Flyway migrates the schema, then a default ADMIN is seeded)
./mvnw spring-boot:run        # or: mvn spring-boot:run

# 3. Open API docs
open http://localhost:8080/api/swagger-ui.html
```

On first start, if the database has no users, an administrator is created from
`CIAP_BOOTSTRAP_ADMIN_EMAIL` and `CIAP_BOOTSTRAP_ADMIN_PASSWORD`. The password
is not written to the log. The docker profile will not create that account
when the password is still the built-in development default, and it will not
start with the development JWT fallback. Set `CIAP_JWT_SECRET` from
`.env.example` before a shared deployment.

## Authentication flow

```bash
# Login (public)
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"<admin-email>","password":"<admin-password>"}'
# -> { data: { accessToken, refreshToken, ... } }

# Refresh when the access token expires (public)
curl -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'

# Use the token on protected endpoints
curl http://localhost:8080/api/clients \
  -H "Authorization: Bearer <accessToken>"
```

## Consultancy request workflow (Module 3)

The blueprint workflow is enforced as a state machine in `RequestStatus`. Illegal
transitions are rejected with HTTP 422.

```
SUBMITTED → UNDER_REVIEW → QUOTED → AWAITING_PAYMENT → PAID
          → ASSIGNED → IN_EXECUTION → REPORT_SUBMITTED → CLOSED
(any non-terminal state → CANCELLED;  UNDER_REVIEW/QUOTED → REJECTED)
```

| Endpoint | Transition | Roles |
|----------|-----------|-------|
| `POST /consultancy-requests`              | create (SUBMITTED) | CLIENT, MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/start-review` | → UNDER_REVIEW | MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/review`  | → QUOTED / REJECTED | MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/await-payment` | → AWAITING_PAYMENT | MANAGER, ADMIN, FINANCE |
| `POST /consultancy-requests/{id}/confirm-payment` | → PAID (GePG hook) | FINANCE, ADMIN |
| `POST /consultancy-requests/{id}/assign`  | → ASSIGNED (+workload) | MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/start-execution` | → IN_EXECUTION | EXPERT, MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/submit-report` | → REPORT_SUBMITTED | EXPERT, MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/close`   | → CLOSED (−workload) | MANAGER, ADMIN |
| `POST /consultancy-requests/{id}/cancel`  | → CANCELLED | CLIENT, MANAGER, ADMIN |

Assigning a request increments the consultant's `activeAssignments` and recomputes
their `availabilityStatus` (AVAILABLE → PARTIALLY_AVAILABLE → FULLY_BOOKED).

## Security notes for production

- Replace `CIAP_JWT_SECRET` with a freshly generated 256-bit key (`openssl rand -base64 32`).
- Replace the dev MySQL password; do not use the seeded admin credentials.
- The reference-number generator in `ConsultancyRequestService` is in-memory; switch to a
  DB sequence/table for gap-free, persistent numbering before go-live.
- `POST /auth/refresh` exchanges a refresh token for a fresh access token (with rotation).
  Token revocation/blacklist is still recommended before go-live.

## Quotation, Contract & Billing (Modules 5, 7, 8)

A request that has reached `UNDER_REVIEW`/`QUOTED` can be priced, contracted and billed:

| Endpoint | Action | Roles |
|----------|--------|-------|
| `POST /quotations` | Create a quotation with line items (computes subtotal, VAT, total) | MANAGER, ADMIN |
| `GET  /quotations/{id}` | Fetch a quotation with its items | MANAGER, ADMIN, FINANCE, CLIENT |
| `GET  /quotations/request/{requestId}` | All quotations for a request | MANAGER, ADMIN, FINANCE, CLIENT |
| `POST /quotations/{id}/send` | DRAFT → SENT | MANAGER, ADMIN |
| `POST /quotations/{id}/accept` | SENT → ACCEPTED; generates a DRAFT contract | CLIENT, MANAGER, ADMIN |
| `POST /payments/issue/{requestId}` | Issue GePG control number; request QUOTED → AWAITING_PAYMENT | FINANCE, MANAGER, ADMIN |
| `POST /payments/gepg-callback` | Settlement callback; payment → PAID, request AWAITING_PAYMENT → PAID | FINANCE, ADMIN |
| `GET  /payments/request/{requestId}` | Payments for a request | FINANCE, MANAGER, ADMIN |

Totals are computed server-side: `lineTotal = quantity × unitPrice`, `subtotal = Σ lineTotal`,
`vatAmount = subtotal × vatRate` (default 18%), `total = subtotal + vatAmount`. The control number
is a 12-digit GePG-style reference. Status changes still flow through the consultancy request
state machine, so billing never bypasses workflow rules. The callback is idempotent (a repeated
settlement for an already-PAID control number is a no-op).

> Production note: the `/payments/gepg-callback` endpoint is currently role-guarded for testing.
> A real GePG integration authenticates the callback by signature, not by user token.

## Project Management (Module 6)

Once a request is billed/assigned (`PAID`, `ASSIGNED`, or `IN_EXECUTION`) a project can be
spawned to deliver the work. Progress is computed automatically from completed tasks.

| Endpoint | Action | Roles |
|----------|--------|-------|
| `POST /projects/from-request/{requestId}` | Create the project (lead defaults to the assigned consultant) | MANAGER, ADMIN |
| `GET  /projects/{id}` | Project with tasks + milestones | MANAGER, ADMIN, EXPERT, FINANCE |
| `GET  /projects/request/{requestId}` | Project for a request | MANAGER, ADMIN, EXPERT, FINANCE |
| `GET  /projects?status=ACTIVE` | Paged list | MANAGER, ADMIN |
| `POST /projects/{id}/start` | PLANNING/ON_HOLD → ACTIVE (sets actual start) | MANAGER, ADMIN |
| `POST /projects/{id}/hold` | ACTIVE → ON_HOLD | MANAGER, ADMIN |
| `POST /projects/{id}/complete` | ACTIVE → COMPLETED (sets actual end) | MANAGER, ADMIN |
| `POST /projects/{id}/cancel` | → CANCELLED (not from COMPLETED) | MANAGER, ADMIN |
| `POST /projects/{id}/tasks` | Add a task (TODO) | MANAGER, ADMIN, EXPERT |
| `PATCH /projects/{id}/tasks/{taskId}/status` | Set TODO/IN_PROGRESS/BLOCKED/DONE; recomputes progress | MANAGER, ADMIN, EXPERT |
| `POST /projects/{id}/milestones` | Add a milestone | MANAGER, ADMIN, EXPERT |
| `POST /projects/{id}/milestones/{milestoneId}/achieve` | Mark achieved | MANAGER, ADMIN, EXPERT |

`progressPercent = round(DONE tasks / total tasks × 100)`, recomputed whenever a task is
added or its status changes. One project per request is enforced by a unique constraint.

## Project Management (Module 6)

Once a consultancy request is won (PAID), assigned, or in execution, a delivery project
can be spawned from it. Project progress is computed automatically from task completion.

| Endpoint | Action | Roles |
|----------|--------|-------|
| `POST /projects/from-request/{requestId}` | Spawn a project (request must be PAID / ASSIGNED / IN_EXECUTION); lead consultant auto-filled from the assignment | MANAGER, ADMIN |
| `GET  /projects/{id}` | Project with tasks & milestones | MANAGER, ADMIN, EXPERT, FINANCE |
| `GET  /projects/request/{requestId}` | Projects for a request | MANAGER, ADMIN, EXPERT, FINANCE |
| `GET  /projects` | Paged list | MANAGER, ADMIN |
| `POST /projects/{id}/start` | PLANNING/ON_HOLD → ACTIVE | MANAGER, ADMIN |
| `POST /projects/{id}/hold` | ACTIVE → ON_HOLD | MANAGER, ADMIN |
| `POST /projects/{id}/complete` | ACTIVE → COMPLETED | MANAGER, ADMIN |
| `POST /projects/{id}/cancel` | → CANCELLED | MANAGER, ADMIN |
| `POST /projects/{id}/tasks` | Add a task (optional assignee + due date) | MANAGER, ADMIN, EXPERT |
| `PATCH /projects/{id}/tasks/{taskId}/status` | Update task status; recomputes project progress | MANAGER, ADMIN, EXPERT |
| `POST /projects/{id}/milestones` | Add a milestone | MANAGER, ADMIN, EXPERT |
| `POST /projects/{id}/milestones/{milestoneId}/achieve` | Mark milestone achieved | MANAGER, ADMIN, EXPERT |

Progress is `round(tasksDone / totalTasks × 100)`, recalculated on every task status change —
so completing one of two tasks moves the project to 50%.

## Reporting & Business Intelligence (Module 12)

Read-only dashboard endpoints, restricted to ADMIN / MANAGER / FINANCE. All aggregation
runs in the database; nothing is loaded into memory to be counted.

| Endpoint | Returns |
|----------|---------|
| `GET /reports/summary`  | Executive KPIs: clients, requests, open requests, active projects, consultants, total revenue |
| `GET /reports/revenue`  | Collected vs pending billed, paid/pending counts, collected-this-year, 12-month collected series |
| `GET /reports/projects` | Total / active / completed, average progress %, breakdown by status |
| `GET /reports/requests` | Pipeline: open vs closed, breakdown by status and by consultancy type |
| `GET /reports/clients`  | Total clients, breakdown by industry and by client type |
| `GET /reports/experts`  | Consultant utilization: available / fully-booked, avg load, breakdown by availability, busiest consultants |

The monthly revenue series always returns a continuous 12-month window (months with no
settlements show zero), so a chart can bind to it directly.

## Reporting & Business Intelligence (Module 12)

Read-only executive dashboards aggregating across every table built so far.
All endpoints are restricted to ADMIN, MANAGER, and FINANCE.

| Endpoint | Dashboard |
|----------|-----------|
| `GET /reports/summary` | Top-line KPIs: clients, requests, open requests, active projects, consultants, revenue collected |
| `GET /reports/revenue` | Collected vs pending billed, paid/pending counts, collected-this-year, last-12-months trend |
| `GET /reports/projects` | Total / active / completed projects, average progress, distribution by status |
| `GET /reports/requests` | Request pipeline: total, open, closed, distribution by status and by consultancy type |
| `GET /reports/clients` | Total clients, distribution by industry category and client type |
| `GET /reports/experts` | Consultant utilization: total, available, fully booked, avg active assignments, top 10 by load |

Aggregations are explicit JPQL `count`/`sum`/`avg`/`group by` queries. The monthly revenue
trend is bucketed in Java from `(paidAt, amount)` pairs, so it needs no database-specific
date functions and stays portable across MySQL versions. Counts are read defensively
(`Number.longValue()`), so they work regardless of how the JPA provider types the result.

## Testing

A test suite covers the business-critical logic and the full pipeline. Tests run against
in-memory H2 (MySQL compatibility mode) under a dedicated `test` profile with Flyway
disabled, so no MySQL instance is needed to run them.

```bash
mvn test
```

| Test class | What it locks down |
|------------|--------------------|
| `RequestStatusTest` | The workflow state machine: legal paths, rejected skips/back-steps, terminal states (pure unit, no Spring) |
| `ConsultantWorkloadTest` | Availability recompute as assignments grow/shrink; no negative load |
| `PipelineIntegrationTest` | End-to-end through the real services: client → request → review → quote (with VAT math) → bill → GePG callback → assign → project → tasks (50% progress) → reporting; plus illegal-transition, bill-before-quote, and idempotent-callback guards |
| `SecurityAccessTest` | Public vs protected endpoints and method-level role rules via MockMvc |

## Next modules (suggested order, matching the blueprint recommendation)

1. **Quotation & Contract** entities + PDF generation, linked from the QUOTED step.
2. **Billing & GePG** — control-number generation and payment-confirmation callback that
   drives `/confirm-payment` automatically.
4. **Reporting/BI** dashboards over the existing tables.
