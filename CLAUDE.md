# CLAUDE.md — Project Briefing

This file is read at the start of every Claude Code session. It orients you to the project. Read the linked docs before writing code.

## What this project is

A B2B monitoring tool that watches the UK **Companies House** register and alerts users when something changes at the companies they care about (new filings, officer changes, address changes, **charges/debts registered**, ownership changes, status changes such as liquidation).

**First target customer: lenders / finance providers** (commercial lenders, invoice-finance, asset-finance firms, finance brokers) who need to know when something risky happens at a company they have lent money to. They do not control these companies, so every change is meaningful signal.

The same engine later serves accountants (deadline-tracking emphasis) and B2B sales teams (buying-signal emphasis). Build the engine once; re-skin per segment later. **Do not build multi-segment features in v1.**

## Tech stack

- **Java 21** + **Spring Boot 3.x**
- **Spring Web** — REST controllers (dashboard API)
- **Spring Data JPA + PostgreSQL** — persistence
- **Spring WebClient** — Companies House REST + Streaming API clients
- **Spring `@Scheduled`** — bulk import + deadline/heartbeat jobs
- **Spring application events** — async in-process processing (NO external message broker in v1)
- **Spring Mail** — email alerts (the only alert channel in v1)
- **Flyway** — database migrations
- **Docker** — containerisation
- **Deploy:** single container on a cheap VPS to start; designed so it can move to a managed cloud container service (AWS ECS / GCP Cloud Run) later. See docs/architecture.md.

## The docs (read these)

- `docs/product-spec.md` — problem, solution, benefits, segments, features, pricing
- `docs/architecture.md` — the event-driven design mapped to Spring components
- `docs/data-sources.md` — Companies House APIs: what each provides, rate limits, gotchas
- `docs/mvp-scope.md` — **strict** in/out list for v1. Read before adding ANY feature.

## How to work on this project

1. **Confirm scope before building.** On a new task, first check it against `docs/mvp-scope.md`. If it is out of scope, say so and stop.
2. **Build one component at a time.** Do not scaffold the whole system at once. Propose the project structure first and get confirmation.
3. **Respect the external API limits** in `docs/data-sources.md` from the start (rate limits, max 2 streaming connections). Design around them; do not discover them later.
4. **Keep v1 simple.** No message broker, no multi-channel alerts, no shareholder/XBRL parsing. When in doubt, prefer the smaller version and flag the bigger one as "later."
5. The developer is an experienced Java engineer. Do not over-explain Java or basic Spring. Focus explanation on architecture decisions, Companies House domain specifics, and trade-offs.
6. Write migrations with Flyway for every schema change. Containerise as you go.

## Definition of v1 "done"

A lender can: create an account, add a list of company numbers to watch, and receive an email when a watched company has a relevant change — plus see those companies and recent events on a simple dashboard. That is the whole v1. Nothing more.
