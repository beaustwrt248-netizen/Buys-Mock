-- Restore the private Admin config implementation boundary after later config changes.
-- Authenticated callers must use public.admin_set_config(...), whose SECURITY DEFINER
-- wrapper delegates to this private implementation after the implementation performs
-- explicit Admin/Manager authorization, validation, and audit logging.

revoke execute on function private.admin_set_config_impl(text, jsonb) from authenticated;
revoke execute on function private.admin_set_config_impl(text, jsonb) from public, anon;

comment on function private.admin_set_config_impl(text, jsonb) is
  'Privileged Admin/Manager config implementation. Direct client execution is revoked; authenticated callers must use public.admin_set_config(text,jsonb).';
