-- Auth is fully delegated to Clerk; the legacy local-password column is unused. Drop it.
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
