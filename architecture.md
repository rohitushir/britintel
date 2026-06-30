# Architecture

## Shape of the system

Event-driven monitoring pipeline:

```
Companies House Bulk snapshot ──(scheduled import)──┐
Companies House REST API ──────(on-demand lookup)───┤
                                                     ▼
                                              [ PostgreSQL ]
                                        users, watched_companies,
                                        company_state, events
                                                     ▲
Companies House Streaming API ─(long-running worker)─┤
        pushes ALL company changes in real time      │
                                                     ▼
                                   match change → is it a watched company?
                                                     │ yes
                                                     ▼
                                   Spring application event (async)
                                                     ▼
                                   persist event + send email alert
                                                     ▼
                                        Dashboard (Spring Web REST) ── user
```

## Components mapped to Spring

| Component | Responsibility | Spring mechanism |
|---|---|---|
| **Bulk importer** | Periodically load/refresh the universe (or at least watched companies) from the monthly bulk product or REST | `@Scheduled` job + WebClient + JPA |
| **REST client** | Look up a single company's current details on demand (when a user adds it; to backfill state) | `WebClient` (or `RestClient`), with rate-limit handling |
| **Streaming worker** | Hold a long-running connection to the Streaming API, consume the firehose of changes, reconnect with back-off | A `@Component` started on app ready, using `WebClient` streaming; resilient reconnect logic |
| **Matcher** | For each incoming change, check if the company is on any user's watch list; drop if not | Service querying an indexed `watched_companies` table |
| **Event store** | Record every relevant change (type, company, timestamp, payload) | JPA entity `events` |
| **Alert dispatcher** | Send an email when a relevant event occurs for a user | Spring `ApplicationEventPublisher` → async `@EventListener` → Spring Mail |
| **Dashboard API** | List watched companies + recent events for the logged-in user | Spring Web REST controllers |
| **Auth** | User accounts and login | Spring Security (keep simple in v1) |

## Key design decisions (and why)

1. **No external message broker in v1.** Use Spring's in-process `ApplicationEventPublisher` + `@Async` listeners. Reason: a single instance can comfortably handle early volume; a broker (RabbitMQ/SQS) adds ops burden with no early payoff. Design the dispatch boundary cleanly so a broker can be slotted in later **if** throughput demands it. Note this seam in code.

2. **The Streaming API is the heart; REST/bulk are for backfill.** The stream gives real-time change events. REST fills in a company's current state when a user first adds it. Bulk is the cheap way to keep a wider cache fresh. Do not poll REST for changes — that is what the stream is for, and polling would blow the rate limit.

3. **Idempotency is mandatory.** Stream events can be redelivered or arrive out of order, and the connection will drop and resume. Never send a duplicate alert. Use the event's unique identifier / `timepoint` to deduplicate before persisting and before emailing. Store the last processed `timepoint` so the stream resumes from where it left off after a restart.

4. **Single long-running connection, respect the 2-connection cap.** The Streaming API allows max 2 concurrent connections per account. v1 uses ONE connection for the firehose. Do not open a connection per user.

5. **State-diff model.** Store each watched company's last-known relevant state. When a change arrives, compare to detect what actually changed, classify the event type (charge / officer / address / status / filing), then decide if it is alert-worthy for that user/segment.

6. **Stateless-friendly web layer, stateful worker.** The REST/dashboard layer can scale horizontally later. The streaming worker is a singleton (only one should hold the connection) — keep it logically separate so deployment can treat them differently down the line.

## Deployment

- **v1:** one Docker container running the whole Spring Boot app + a PostgreSQL instance (managed Postgres or a container), on a cheap VPS. Flyway runs migrations on startup.
- **Later:** the web layer and the streaming worker can be split into separate deployables; move to a managed container service (AWS ECS / GCP Cloud Run) and managed Postgres (RDS / Cloud SQL). Keep the streaming worker as a single instance even when scaling the web tier.
- Use environment variables / Spring profiles for config (API keys, DB creds, mail creds). Never hardcode secrets.

## What you will learn building this

Scheduled jobs, long-running resilient connections, back-off/reconnect, idempotent event processing, async event handling, relational data modelling of changing state, REST API design, containerised deployment, and a clean seam for later horizontal scaling. This is a real event-driven cloud system.
