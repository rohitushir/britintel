-- Landing-page signal capture: lenders who click the CTA leave an email so we can onboard them.
-- This is a demand test, not a product feature — every row is one measurable signal.
CREATE TABLE early_access_signups (
    id         BIGSERIAL   PRIMARY KEY,
    email      TEXT        NOT NULL,
    source     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One signal per email (case-insensitive); repeated submits are idempotent.
CREATE UNIQUE INDEX ux_early_access_email ON early_access_signups (lower(email));
