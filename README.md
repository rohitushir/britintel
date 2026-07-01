# BritIntel

B2B monitoring for the UK **Companies House** register. A lender adds the company numbers
they've lent to; the system watches Companies House and **emails an alert the moment a relevant
change happens** (new charge, status → liquidation, officer change, address change, new filing),
with a dashboard of watched companies and recent events.

See `product-spec.md`, `architecture.md`, `data-sources.md`, `mvp-scope.md` for the full briefing.

## Stack

Java 21 · Spring Boot 3.5 (Web, WebFlux, Data JPA, Security, Mail) · PostgreSQL + Flyway · Docker.

## Architecture at a glance

```
Streaming API ──(resilient worker, resumes from timepoint)──┐
REST API ──────(backfill on add, rate-limited)──────────────┤
                                                             ▼
                       match (watched?) → classify (state diff) → persist idempotently
                                                             ▼
                       Spring app event (after commit, async) → email alert
                                                             ▼
                                  Dashboard REST API + static page ── user
```

Package map: `account` (auth), `watchlist`, `company` (state + backfill), `companieshouse/rest`
(+ rate limiter), `companieshouse/streaming` (worker), `processing` (matcher/classifier),
`alerts` (dispatch + notifier), `dashboard`, `config`, `web`.

## Run locally (no Companies House key)

```bash
docker compose up -d db          # Postgres on host port 5544
set -a; . ./.env; set +a         # after: cp .env.example .env
mvn spring-boot:run
```

Open http://localhost:8080 — sign up via Clerk and add a company (sign-in needs the `CLERK_*`
keys set). Without a REST key, "add" returns 502 (backfill can't reach Companies House); everything
else works. The streaming worker auto-skips when no streaming key is set.

## Run fully containerised

```bash
cp .env.example .env             # fill in keys
docker compose up --build        # app + Postgres; Flyway migrates on startup
```

## Configuration (env vars)

| Var | Purpose |
|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | Postgres connection |
| `CH_REST_API_KEY` | Public Data (REST) API — backfill on add |
| `CH_STREAM_API_KEY` | Streaming API — the live change feed (worker skips if empty) |
| `CH_STREAM_NAMES` | Streams to consume, max 2 (default `companies,charges`) |
| `ALERTS_EMAIL_ENABLED` | `true` to send real email |
| `ALERTS_EMAIL_FROM` | From address for alerts (must match the SMTP mailbox) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP provider |
| `MAIL_SMTP_STARTTLS` / `MAIL_SMTP_SSL` | `587`→STARTTLS=true,SSL=false · `465`→SSL=true,STARTTLS=false |
| `CLERK_PUBLISHABLE_KEY` / `CLERK_SECRET_KEY` | Clerk auth (secret is server-side only) |
| `CLERK_FRONTEND_API_URL` | Clerk instance URL — JWKS/issuer + frontend SDK source |
| `APP_BASE_URL` | Absolute app URL for the dashboard link in alert emails (optional) |

## Going live — verification runbook (step 8)

With real keys in `.env`:

1. **REST backfill.** Sign in (Clerk), add a real company number (e.g. `00000006`). Expect 201
   and the company name populated from Companies House. Check `company_state` has a row.
2. **Streaming.** Set `CH_STREAM_API_KEY` and restart. Log shows
   `Streaming worker started 2 connection(s): [COMPANIES, CHARGES]`. Watch a company you can change (or a
   busy one) and confirm an `events` row + an alert (logged, or emailed if email enabled).
3. **No duplicate alerts.** Force a reconnect (restart the app). The worker resumes from the
   persisted `stream_position`; replayed events must NOT create duplicate `events` rows or
   re-send alerts (guaranteed by `events.dedup_key`).
4. **Resume across restart.** Note `stream_position.last_timepoint`, restart, confirm it resumes
   from there rather than the head.
5. **Rate limit.** Backfill several companies in quick succession; the REST limiter keeps you
   under ~2/sec (no 429s).

## Deploying to production

The app is built to run as a single container behind a TLS-terminating reverse proxy. It is
stateless (auth is Clerk JWTs), so the only stateful piece is Postgres.

**Before you deploy:**

1. **Clerk production instance.** The dev keys (`pk_test_…`, `*.clerk.accounts.dev`) are for
   development only. Create a **production** instance in the Clerk dashboard, point it at your
   domain, and set `CLERK_PUBLISHABLE_KEY` (`pk_live_…`), `CLERK_SECRET_KEY` (`sk_live_…`), and
   `CLERK_FRONTEND_API_URL` to the production Frontend API URL. Add your domain to Clerk's allowed
   origins. (The JWT decoder validates the token issuer against `CLERK_FRONTEND_API_URL`.)
2. **HTTPS is required** — Clerk production and bearer tokens must not run over plain HTTP. Put a
   reverse proxy (Caddy/nginx/Traefik) in front that terminates TLS and forwards `X-Forwarded-*`
   (the app already honours them via `forward-headers-strategy`). Set `APP_BASE_URL=https://your.domain`.
3. **Secrets** live in the server's environment (or a secrets manager), never in the image or git.
   `.env` is gitignored and excluded from the Docker build via `.dockerignore`.
4. **Database:** do not expose Postgres to the internet (compose binds it to `127.0.0.1`), and set
   up backups (`pg_dump`/managed snapshots). Consider a managed Postgres for real workloads.
5. **Run a single app instance.** The streaming worker holds the Companies House connections and is
   capped at 2 — do **not** horizontally scale the app or you'll exceed the cap. Scale the DB, not
   the streamer.

**Deploy:**

```bash
cp .env.example .env            # fill in PROD secrets (Clerk live keys, SMTP, CH keys)
docker compose up -d --build    # Flyway migrates on startup
```

The container has a `HEALTHCHECK` (actuator `/health`) and `restart: unless-stopped`, so it
self-heals and survives reboots. Graceful shutdown drains in-flight requests and closes the stream
cleanly. Health/readiness: `GET /actuator/health`.

## Tests

```bash
docker compose up -d db
set -a; . ./.env; set +a
mvn test
```

Unit + integration tests cover the rate limiter, REST client parsing/404, the streaming
reconnect/resume/stale-timepoint loop, message parsing, the auth + watch-list flow (with the CH
client mocked), and the match → classify → idempotent-persist pipeline against a real database.
