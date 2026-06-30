# MVP Scope (v1)

**Read this before adding ANY feature.** If a request is not in "IN SCOPE" below, it is out — say so and stop, or flag it as "later" and confirm before building.

## The v1 goal (one sentence)

A lender can create an account, add a list of company numbers to watch, and receive an **email** when a watched company has a relevant change — and see those companies and recent events on a simple dashboard.

## IN SCOPE for v1

- **User accounts + login** (Spring Security, simple).
- **Watch list management** — add/remove company numbers; per-account cap on count.
- **Backfill on add** — when a company is added, fetch its current state via the REST API and store it.
- **Streaming worker** — one resilient connection to the Streaming API, consuming changes, resuming from last `timepoint`.
- **Matching** — drop changes for companies nobody watches; keep the rest.
- **Event classification** — charge / status / officer / address / filing (see data-sources.md).
- **Idempotent processing** — never persist or alert on a duplicate.
- **Email alerts** — one clear email per relevant event (or a sensible digest) to the watching user.
- **Dashboard** — list watched companies + recent events for the logged-in user (Spring Web REST + a minimal front end or JSON API; keep UI minimal).
- **Postgres + Flyway migrations.**
- **Dockerised app.**
- **Config via env vars / Spring profiles.**

## OUT OF SCOPE for v1 (do NOT build)

- Slack, webhooks, SMS, or any alert channel other than email.
- Billing / payment integration (onboard first paying users manually).
- The accountant and sales-team segments (and their segment-specific features).
- Deadline-tracking / filing-deadline calendar (that is an accountant-segment feature).
- CRM / Zapier integration, data enrichment, territory lists.
- Shareholder parsing, XBRL/accounts financial analysis, document (PDF) parsing.
- Multiple streaming connections; per-user connections.
- An external message broker (RabbitMQ/SQS) — use in-process Spring events.
- Risk scoring / ML / "risk flags" beyond simple event-type prioritisation.
- Multi-region, horizontal scaling work (design the seam, don't build it).
- Anything requiring data Companies House does not expose via API.

## Build order (suggested)

1. Project skeleton + Postgres + Flyway + Docker; confirm structure first.
2. Data model: users, watched_companies, company_state, events.
3. REST client (with rate limiter) + "add a company → backfill state" flow.
4. Auth + watch-list management + minimal dashboard (read existing data).
5. Streaming worker: connect, consume, resume from timepoint, reconnect/back-off.
6. Matching + event classification + idempotent persistence.
7. Email alert dispatch (async via Spring events).
8. End-to-end test with real watched companies; harden reconnect + dedup.

## Definition of done

All eight steps working against the live Companies House APIs, for a real lender watching a real list of borrower companies, with no duplicate alerts and a stream that survives a restart. Stop there. Validate with users before extending.
