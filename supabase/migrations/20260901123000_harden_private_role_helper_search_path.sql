-- Defense-in-depth hardening for private role helpers used by RLS policies.
--
-- Both functions already live in the non-exposed private schema, reference
-- public.profiles with an explicit schema qualifier, and are not executable by
-- anon. Pin an empty search_path so SECURITY DEFINER name resolution cannot be
-- influenced by caller-controlled schemas.

alter function private.is_admin()
  set search_path = '';

alter function private.is_admin_or_manager()
  set search_path = '';

-- Preserve the existing least-privilege execution boundary explicitly.
revoke execute on function private.is_admin() from public, anon;
revoke execute on function private.is_admin_or_manager() from public, anon;
grant execute on function private.is_admin() to authenticated;
grant execute on function private.is_admin_or_manager() to authenticated;
