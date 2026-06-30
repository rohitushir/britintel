-- Core v1 data model: users, watched_companies, company_state, events.
-- See architecture.md (state-diff model, idempotency) and data-sources.md (event types).

-- ---------------------------------------------------------------------------
-- users: accounts + login. Spring Security wiring arrives in step 4.
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         TEXT        NOT NULL,
    password_hash TEXT        NOT NULL,
    -- per-account cap on number of watched companies (pricing tiers; no billing in v1).
    company_cap   INTEGER     NOT NULL DEFAULT 50,
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Case-insensitive unique email.
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

-- ---------------------------------------------------------------------------
-- watched_companies: each user's watch list. A company may be watched by many users.
-- ---------------------------------------------------------------------------
CREATE TABLE watched_companies (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    company_number TEXT        NOT NULL,
    -- cached display name; filled in on backfill (step 3), nullable until then.
    company_name   TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_watch_user_company UNIQUE (user_id, company_number)
);

-- Matcher hot path: given a change for a company number, find every watcher fast.
CREATE INDEX ix_watched_company_number ON watched_companies (company_number);

-- ---------------------------------------------------------------------------
-- company_state: last-known relevant state per company, for the state-diff model.
-- One row per company_number (shared across all watchers of that company).
-- ---------------------------------------------------------------------------
CREATE TABLE company_state (
    company_number    TEXT PRIMARY KEY,
    company_name      TEXT,
    company_status    TEXT,            -- active / liquidation / dissolved / ...
    registered_office JSONB,           -- structured address, for address-change diffs
    date_of_creation  DATE,
    raw_profile       JSONB,           -- full profile snapshot for reference / future diffs
    -- last stream timepoint that updated this row (helps ordering / debugging).
    last_timepoint    BIGINT,
    fetched_at        TIMESTAMPTZ,     -- when the REST backfill last refreshed this state
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- events: every relevant change at a watched company. Idempotency via dedup_key.
-- Dashboard shows recent events for the companies a user watches (join by number).
-- ---------------------------------------------------------------------------
CREATE TABLE events (
    id             BIGSERIAL PRIMARY KEY,
    company_number TEXT        NOT NULL,
    event_type     TEXT        NOT NULL,  -- CHARGE_CREATED, STATUS_CHANGE, ... (EventType enum)
    resource_kind  TEXT        NOT NULL,  -- COMPANY_PROFILE, CHARGES, OFFICERS, FILING_HISTORY
    timepoint      BIGINT,                -- stream position that produced this event
    -- Deterministic key built by the classifier; the unique constraint guarantees we
    -- never persist (or alert on) the same change twice across reconnects/redeliveries.
    dedup_key      TEXT        NOT NULL,
    summary        TEXT        NOT NULL,  -- human-readable line for the alert email/dashboard
    payload        JSONB,                 -- raw change payload for reference
    occurred_at    TIMESTAMPTZ,           -- when the change happened at Companies House
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_events_dedup UNIQUE (dedup_key)
);

-- Dashboard: recent events for a company (and, by join, for a user's watch list).
CREATE INDEX ix_events_company_created ON events (company_number, created_at DESC);
