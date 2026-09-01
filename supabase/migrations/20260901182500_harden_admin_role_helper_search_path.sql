-- Harden private Admin role helpers against search_path object shadowing.
-- This migration is intentionally narrow: it preserves existing role semantics,
-- keeps the helpers in the private schema, and does not broaden execution grants.

create schema if not exists private;
revoke all on schema private from public;

create or replace function private.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.is_enabled = true
      and p.role = 'admin'
  );
$$;

create or replace function private.is_admin_or_manager()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.profiles p
    where p.id = (select auth.uid())
      and p.is_enabled = true
      and p.role in ('admin', 'manager')
  );
$$;

-- Preserve explicit least-privilege execution: authenticated callers need these
-- predicates for RLS; public/anon must not execute them directly.
revoke execute on function private.is_admin() from public, anon;
revoke execute on function private.is_admin_or_manager() from public, anon;
grant execute on function private.is_admin() to authenticated;
grant execute on function private.is_admin_or_manager() to authenticated;