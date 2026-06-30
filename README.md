# CompaniesWatch

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

Open http://localhost:8080 — register, log in, add a company. Without a REST key, "add" returns
502 (backfill can't reach Companies House); everything else works. The streaming worker auto-skips
when no streaming key is set.

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
| `CH_STREAM_NAMES` | Streams to consume, max 2 (default `companies`; add `charges`) |
| `ALERTS_EMAIL_ENABLED` | `true` to send real email (also set `spring.mail.*`) |
| `ALERTS_EMAIL_FROM` | From address for alerts |

## Going live — verification runbook (step 8)

With real keys in `.env`:

1. **REST backfill.** Register, log in, add a real company number (e.g. `00000006`). Expect 201
   and the company name populated from Companies House. Check `company_state` has a row.
2. **Streaming.** Set `CH_STREAM_API_KEY` and restart. Log shows
   `Streaming worker started 1 connection(s): [COMPANIES]`. Watch a company you can change (or a
   busy one) and confirm an `events` row + an alert (logged, or emailed if email enabled).
3. **No duplicate alerts.** Force a reconnect (restart the app). The worker resumes from the
   persisted `stream_position`; replayed events must NOT create duplicate `events` rows or
   re-send alerts (guaranteed by `events.dedup_key`).
4. **Resume across restart.** Note `stream_position.last_timepoint`, restart, confirm it resumes
   from there rather than the head.
5. **Rate limit.** Backfill several companies in quick succession; the REST limiter keeps you
   under ~2/sec (no 429s).

## Tests

```bash
docker compose up -d db
set -a; . ./.env; set +a
mvn test
```

Unit + integration tests cover the rate limiter, REST client parsing/404, the streaming
reconnect/resume/stale-timepoint loop, message parsing, the auth + watch-list flow (with the CH
client mocked), and the match → classify → idempotent-persist pipeline against a real database.
