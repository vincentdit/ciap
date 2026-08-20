# CIAP Frontend (Angular)

Angular 18 (standalone components) frontend for the **Consultancy & Industrial
Advisory Portal** — TIRDO. It talks to the Spring Boot backend over HTTP.

## What's in this first cut

- **Auth**: login, JWT storage, automatic refresh-on-401, route guard, logout.
- **Client self-registration**: full form mapping `ClientRegistrationRequest`,
  creating a client + linked login.
- **Consultancy requests**: submit a new request, list requests (paginated),
  dashboard overview with status counts.
- **Layout**: institutional shell with sidebar nav and session display.

## Prerequisites

- Node.js 18.19+ or 20+ and npm
- The CIAP backend running on `http://localhost:8080` (context path `/api`)

## Run it

```bash
npm install
npm start
```

`npm start` runs `ng serve` with `proxy.conf.json`, which forwards `/api`
to `http://localhost:8080`, so there are no CORS issues in development.
(The backend also already allows the `http://localhost:4200` origin, so direct
calls work too.)

Then open http://localhost:4200.

- Sign in with the seeded admin (`admin@tirdo.go.tz` / `Admin@12345`) or
- Register a new client organisation at `/register`, then sign in.

## How it connects to the backend

- `environment.development.ts` / `environment.ts` set `apiBaseUrl` to `/api`.
- The auth interceptor attaches `Authorization: Bearer <token>` to every API
  call except `/auth/login` and `/auth/refresh`, and on a 401 it refreshes the
  token once and retries.
- TypeScript models in `core/models/api.models.ts` mirror the backend DTOs
  (`ApiResponse` envelope, `PageResponse`, auth, client, request).

## Project structure

```
src/app/
  core/
    auth/         AuthService (token + session signals)
    api/          ClientApi, RequestApi
    guards/       authGuard
    interceptors/ authInterceptor
    models/       api.models.ts (mirrors backend DTOs)
  features/
    auth/         login
    register/     client self-registration
    dashboard/    overview + stats
    requests/     new-request, request-list
  layout/         shell (authenticated chrome)
```

## Known follow-ups

- **Client ID on new requests**: the new-request form currently asks for the
  client ID explicitly. A backend `GET /clients/me` (resolving the client from
  the authenticated user — the repository already has `findByUserId`) would let
  the form prefill it. Worth adding next.
- **Staff workflow (built)**: non-CLIENT roles see a "Staff → Request Queue"
  nav entry. The queue (`/staff/requests`) lists all requests with status
  filters; opening one (`/staff/requests/:id`) shows role-aware workflow action
  buttons that mirror the backend state machine and @PreAuthorize guards —
  start-review, review (approve/reject + notes), await-payment, confirm-payment,
  assign consultant, start-execution, submit-report, close, cancel. Managers can
  build quotations (`/staff/requests/:id/quote`, live VAT/total preview), finance
  can record GePG settlements, and managers can spawn and run delivery projects
  (`/staff/projects/:id`: status control, tasks with status updates, milestones,
  progress).
- Production build is served at `/api` on the same origin; put Angular's
  `dist/` behind the same reverse proxy as the backend for deployment.
```

## Build for production

```bash
npm run build
```

Output goes to `dist/ciap-frontend/`.
