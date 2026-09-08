-- Restore the authenticated execute grant required by the existing public
-- admin_set_config SECURITY INVOKER wrapper. The private implementation remains
-- SECURITY DEFINER and performs its own admin/manager authorization and audit.

grant usage on schema private to authenticated;
grant execute on function private.admin_set_config_impl(text,jsonb) to authenticated;

revoke all on function private.admin_set_config_impl(text,jsonb) from public, anon;

comment on function private.admin_set_config_impl(text,jsonb) is
  'Privileged validated Admin/Manager config implementation. Execute is granted only to authenticated; the function performs its own admin/manager authorization and audit logging.';
