-- Resume points for the streaming worker. One row per stream; the worker persists the last
-- processed timepoint so it can resume from where it left off after a disconnect or restart
-- without missing changes (architecture.md: idempotency + resume). Dedup (events.dedup_key)
-- absorbs any small overlap from resuming slightly early.
CREATE TABLE stream_position (
    stream_name    TEXT PRIMARY KEY,      -- e.g. 'companies', 'charges'
    last_timepoint BIGINT,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
