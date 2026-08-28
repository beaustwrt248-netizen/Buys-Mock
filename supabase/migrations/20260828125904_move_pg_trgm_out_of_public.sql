create schema if not exists extensions;
revoke create on schema extensions from public;
alter extension pg_trgm set schema extensions;
