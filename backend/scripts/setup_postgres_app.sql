-- Run against Postgres.app:
--   psql -d postgres -f backend/scripts/setup_postgres_app.sql

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'onigiri') THEN
    CREATE ROLE onigiri LOGIN PASSWORD 'onigiri';
  END IF;
END
$$;

SELECT 'CREATE DATABASE onigiri OWNER onigiri'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'onigiri')\gexec

SELECT 'CREATE DATABASE onigiri_test OWNER onigiri'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'onigiri_test')\gexec
