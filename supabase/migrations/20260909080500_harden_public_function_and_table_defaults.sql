-- Harden public-schema defaults so client roles do not inherit privileged
-- operations on future objects through PostgreSQL's default PUBLIC grants.

-- Trigger helpers are not client RPC endpoints.
revoke execute on function public.refresh_laptop_model_search_text()
  from public, anon, authenticated;

-- New public functions must be explicitly granted to the roles that need them.
alter default privileges for role postgres in schema public
  revoke execute on functions from public, anon, authenticated;

-- RLS does not cover these table-level operations. Keep them out of future
-- anon/authenticated grants unless a migration explicitly opts them in.
alter default privileges for role postgres in schema public
  revoke truncate, references, trigger, maintain on tables from anon, authenticated;
