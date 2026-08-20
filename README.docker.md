# CIAP — Local Docker stack

Runs the whole system in Docker: **MySQL 8.4 + Spring Boot backend + Angular
(built and served by Nginx)**. No Java, Maven, or Node needed on the host —
only Docker Desktop.

## Layout expected

```
ciap/
  docker-compose.yml
  .env
  backend/           <- Spring Boot project (pom.xml, ./mvnw, src/)
    Dockerfile
    src/main/resources/application-docker.yml   (added)
  frontend/          <- Angular project (package.json, angular.json, src/)
    Dockerfile
    nginx.conf
```

Drop the Docker files into your existing `backend/` and `frontend/` folders and
the compose files into the `ciap/` root.

## Run it

```bash
cd "C:\Users\USER\My Drive\ciap"
docker compose up --build
```

First build takes a few minutes (downloads Maven deps and the npm toolchain,
compiles both apps). When it settles, open:

- **App:**      http://localhost:8088
- **API:**      http://localhost:8080/api  (direct, for testing)
- **MySQL:**    localhost:3307 (user/pass from `.env`)

Sign in with `admin@tirdo.go.tz` / `Admin@12345` (unless you changed the admin
env vars and your DataInitializer reads them).

Stop with `Ctrl+C`; to also remove containers: `docker compose down`.
To wipe the database and start fresh: `docker compose down -v` (the `-v` drops
the volumes, so MySQL and uploaded documents are erased).

## How the pieces connect

- The browser talks only to **Nginx** (port 8088). Nginx serves the Angular
  bundle and proxies `/api/...` to the **backend** container on the compose
  network — so there are no CORS issues and no backend port needed by the browser.
- The backend connects to **db** (MySQL) by the service name on the compose
  network. It waits for MySQL's healthcheck before starting.
- **Flyway** runs V1–V7 against the fresh database on first backend start.
- Two named volumes persist state: `ciap-db-data` (MySQL) and `ciap-docs`
  (uploaded documents). They survive `down` but not `down -v`.

## Configuration (.env)

All secrets and DB credentials live in `.env`. **These are dev defaults —
change the JWT secret and passwords before any shared/real deployment.**
Generate a strong JWT secret:

```bash
openssl rand -base64 48
```

## Troubleshooting

- **API calls 404 through the app but work on :8080/api directly** — the
  backend's context-path isn't `/api` as the Nginx proxy assumes. Check
  `server.servlet.context-path` in your base `application.yml`; tell me the
  value and I'll adjust `nginx.conf`.
- **Backend exits with a Flyway "validate" or schema error** — the `docker`
  profile uses `ddl-auto: validate` (Flyway owns the schema). If your entities
  and migrations disagree, that surfaces here. Run `mvn test` locally (H2) to
  pinpoint it.
- **Backend can't connect to MySQL** — usually a startup race; compose already
  gates on the healthcheck, but if MySQL is slow to initialise on first run,
  `docker compose up` again once the db is up.
- **Port already in use** — something else holds 8088/8080/3307. Change the
  left-hand side of the port mappings in `docker-compose.yml`.
- **Frontend build fails on `npm ci`** — your `package-lock.json` must be
  committed and in sync with `package.json`. Run `npm install` once locally to
  refresh it, then rebuild.
