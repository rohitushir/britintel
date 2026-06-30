# Product Spec

## The problem (lender framing — v1)

A lender, invoice-finance provider, or finance broker has money out across many borrower companies. Things happen at those companies that directly threaten repayment, and the lender usually has no easy way to find out in time:

- A **charge** is registered against the borrower — someone else now has a claim on the borrower's assets, possibly ranking alongside or ahead of the lender.
- A **director resigns** or the board changes — a sign of instability or a control change.
- The **registered office address changes** — sometimes a red flag.
- The company **status changes** toward liquidation/insolvency — the single most important event.
- New **filings** appear (accounts, confirmation statements) that may carry warning signs.

Today the lender either checks Companies House manually company-by-company (nobody has time across a whole loan book), relies on Companies House's clunky free per-company "Follow" emails (one company at a time, no portfolio view), or finds out too late. Late awareness costs real money.

## The solution

A tool where the lender adds their list of borrower company numbers once. After that, the system watches Companies House for them and **emails an alert the moment a relevant change happens** at any watched company. A simple dashboard shows all watched companies and recent events in one place.

One sentence: **"Add the companies you've lent to once, and we watch Companies House for you — flagging charges, director changes, and trouble signs the moment they happen."**

## Benefits to the user

- **Catch risk early** — a new charge or a slide toward insolvency, surfaced immediately, not discovered at the next manual review.
- **Protect money owed** — earlier awareness means earlier action to protect a position.
- **Save time** — no manual checking across a whole loan book.
- **Peace of mind / one place** — the whole portfolio's status visible at a glance, nothing relying on memory.

The emotional core: turning "I hope nothing's gone wrong with any of my borrowers" into "I'll be told the instant something changes."

## Segments (build order — DO NOT build segments 2 and 3 in v1)

| | **Lenders (v1)** | **Accountants (later)** | **Sales / BizDev (later)** |
|---|---|---|---|
| Headline feature | Charge & insolvency alerts on borrowers | Filing-deadline tracking across client portfolio | Buying-signal alerts (new appointments, growth filings) |
| Killer event | New charge; status → liquidation | Confirmation statement / accounts due soon | New senior appointment, incorporation activity |
| Extras they want | Risk overview, export for records | Deadline calendar, client-ready summaries | CRM/Zapier push, enrichment |
| De-emphasise | Deadlines (not their job) | "Accounts filed" alerts (they filed them) | Charges/insolvency |

The engine (watch list + change detection + alerts + dashboard) is identical across all three. Segments differ only in which events lead and the wording around them. This is a re-skin, not a rebuild.

## Pricing (for reference — not built in v1 beyond basic account/limits)

Priced by **number of companies watched** + alert/integration capability:

- **Starter ~£29/mo** — up to ~50 companies, email alerts, dashboard. (Solo brokers, individuals.)
- **Pro ~£79/mo** — up to ~250 companies, email + Slack, risk flags, export. (Active lenders, small teams.)
- **Business ~£199/mo** — up to ~1,000 companies, webhooks/API/CRM, team seats, custom rules. (Finance firms with real loan books.)

Pricing logic: value-based, not cost-based — catching one borrower's charge early can save thousands, so these prices are easily justified. Anchor lenders toward Pro/Business. The £29 tier mainly serves the later accountant/solo segment.

**v1 note:** the only pricing-related thing to build in v1 is a per-account cap on number of watched companies. No billing integration in v1 (manual onboarding of first paying users is fine).
