-- Baseline migration for companieswatch.
--
-- Step 1 (skeleton) intentionally creates no domain tables — this migration only proves the
-- Flyway pipeline runs on startup and records a schema-history row. The real data model
-- (users, watched_companies, company_state, events) arrives in step 2 as V2__*.sql.
DO $$
BEGIN
    RAISE NOTICE 'companieswatch Flyway baseline applied';
END $$;
