# ShorTLinks

A full-stack URL shortener with click tracking.

- **Backend:** Java 17, Spring Boot 3, Spring Data JPA, Bean Validation
- **Frontend:** React 18 + Vite
- **Database:** H2 file database by default, PostgreSQL via Docker Compose
- **Tests:** JUnit 5, Mockito, MockMvc (end-to-end API tests)

## Features

- Shorten any `http`/`https` URL to a 7-character code
- Optional custom alias and optional expiry date
- Redirects with click counting (count and last-visited time)
- Expired links return `410 Gone`
- List, copy and delete links from a React dashboard

## Project structure

```
shortlinks/
  backend/    Spring Boot REST API + redirect endpoint
  frontend/   React dashboard (Vite)
  docker-compose.yml
```

Backend layers: `web` (controllers) -> `service` (rules and validation) -> `repository` (JPA).

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/links` | Create a link. Body: `{"url": "...", "alias": "optional", "expiresAt": "optional ISO-8601"}` |
| GET | `/api/links` | List the 100 newest links |
| GET | `/api/links/{code}` | Stats for one link |
| DELETE | `/api/links/{code}` | Delete a link |
| GET | `/{code}` | Redirect (302) to the original URL and record a click |

Errors use the RFC 7807 format (`ProblemDetail`): `400` invalid input, `404` unknown code,
`409` alias taken, `410` link expired.

## Run locally

Requirements: JDK 17, Maven, Node 18+.

Backend (terminal 1):

```bash
cd backend
mvn spring-boot:run
```

Frontend (terminal 2):

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The dev server proxies `/api` to the backend on port 8080.

Quick API check (PowerShell):

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/links -Method Post -ContentType "application/json" -Body '{"url":"https://example.com"}'
```

## Run with Docker (PostgreSQL)

```bash
docker compose up --build
```

Frontend: http://localhost:3000, API: http://localhost:8080.

## Tests

```bash
cd backend
mvn test
```

## Design decisions

- **302, not 301:** browsers cache permanent redirects and would skip the server, so clicks would not be counted.
- **Atomic click counter:** a single `UPDATE ... SET click_count = click_count + 1` avoids lost updates under concurrency.
- **Unique constraint is the source of truth:** a race on the same alias is caught and returned as `409`.
- **Validation in two layers:** Bean Validation on the request, business rules (scheme, host, reserved aliases, expiry) in the service.
- **Configurable via environment variables:** `DB_URL`, `DB_USER`, `DB_PASSWORD`, `APP_BASE_URL`, `CORS_ALLOWED_ORIGIN`.

## Ideas for next steps

1. QR code for each short link
2. Per-click analytics table (timestamp, referrer, user agent) and a chart
3. Rate limiting and API authentication / user accounts
4. Block unsafe destinations (private IPs, known-bad domains)
5. Frontend tests with Vitest and React Testing Library
6. Database migrations with Flyway instead of `ddl-auto: update`
