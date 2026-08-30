-- Defense-in-depth for SECURITY DEFINER role helpers used by authenticated RLS paths.
-- The private schema is already unavailable to anon, but explicit EXECUTE revocation
-- prevents these helpers becoming anonymously callable if schema grants drift later.

revoke execute on function private.is_admin() from public, anon;
revoke execute on function private.is_admin_or_manager() from public, anon;

-- Authenticated access is intentionally preserved because RLS policies use these helpers.
grant execute on function private.is_admin() to authenticated;
grant execute on function private.is_admin_or_manager() to authenticated;
