# Data Sources — Companies House

All data comes from official Companies House APIs. No scraping. Register for an API key at the Companies House developer portal before building, and test against real data early.

## The four access methods

### 1. Public Data API (REST) — on-demand lookups
- Query any company by number; returns JSON.
- Endpoints you will use: company profile, officers, filing history, charges, persons-with-significant-control (PSC), registered office address.
- **Auth:** API key (HTTP Basic, key as username, blank password).
- **Rate limit:** ~600 requests per 5-minute window, across all endpoints combined (~2/sec). Exceeding returns HTTP 429; the window then resets. **Build a rate limiter / throttle around the client from day one.** Higher limits available on request later.
- **Use in this project:** backfill a company's current state when a user adds it to their watch list; refresh state as needed. NOT for change polling.

### 2. Streaming API — real-time change feed (THE CORE)
- A long-running HTTP connection that **pushes** changes as they happen across ALL UK companies.
- Streams available: company profile, filing history, officers, charges (and PSC).
- Each event carries a `timepoint` (a monotonic position marker). Persist the last processed `timepoint` so you can resume the stream after a disconnect/restart without missing or re-alerting.
- **Auth:** streaming API key.
- **Limit:** **max 2 concurrent connections per account.** v1 uses ONE. Implement reconnect with exponential back-off on drop/429.
- **Use in this project:** the primary source of change events. Consume firehose → match against watched companies → emit alert events.

### 3. Document API — filed PDFs
- Download the actual filed documents (accounts, resolutions, etc.).
- **Not needed for v1.** Listed for awareness only.

### 4. Bulk / open data products — cheap backfill
- Monthly CSV snapshot of all active companies (number, name, status, SIC codes, registered address, incorporation date, former names).
- **Use in this project:** optional — to cheaply pre-populate / refresh a wider company cache without hammering the REST API. v1 can rely on REST for just the watched companies if simpler; revisit bulk if REST volume grows.

## What the data contains (per company)

- **Profile:** name, number, status (active / dissolved / liquidation / etc.), type, incorporation date, registered office address, SIC codes, accounting reference dates.
- **Officers:** directors/secretaries — names, roles, appointment/resignation dates, nationality, occupation, partial DOB.
- **Filing history:** every filed document with date and type.
- **PSC:** ultimate owners/controllers in **ownership bands** (e.g. "75%+"); direct controller only.
- **Charges:** mortgages/secured debts — lender, created date, status (outstanding/satisfied), what is secured.
- **Address history.**
- **Insolvency / liquidation details** where applicable.

## The events that matter for lenders (v1 alert types)

Classify incoming stream changes into these alert-worthy types:
1. **Charge created / satisfied** — highest priority for lenders.
2. **Company status change** (esp. toward liquidation/insolvency) — highest priority.
3. **Officer appointed / resigned.**
4. **Registered office address change.**
5. **New filing** (accounts, confirmation statement) — lower priority; may carry signals.

## Known gaps / gotchas (design around these — do NOT try to solve in v1)

- **No shareholder list in the API.** Shareholdings live inside confirmation-statement PDF documents, not as structured fields. Out of scope.
- **PSC is banded, not exact** — not a true ultimate-beneficial-owner tree. Treat as indicative only.
- **Accounts are inconsistently machine-readable** — only ~half are structured XBRL; the rest are PDFs. No automated financial analysis in v1.
- **No personal contact data** — no emails/phones; company data only.
- **Data is free and public.** The data is NOT the moat — anyone can pull it. The product is the watching/matching/alerting workflow on top. Keep this in mind when prioritising.

## Licensing

Companies House data carries no commercial-use restriction for this purpose — fine to build a paid product on. Always confirm current terms on the developer portal before launch; rate limits and registration states can change.
