-- Auth moves to Clerk. Users are provisioned just-in-time from a verified Clerk session token,
-- keyed by the Clerk user id (the JWT `sub`). Passwords are no longer stored.
ALTER TABLE users ADD COLUMN clerk_user_id varchar(255);
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- One local account per Clerk user. Legacy rows keep a NULL clerk_user_id (Postgres unique
-- indexes allow multiple NULLs), so they simply never match a Clerk login.
CREATE UNIQUE INDEX ux_users_clerk_user_id ON users (clerk_user_id);
