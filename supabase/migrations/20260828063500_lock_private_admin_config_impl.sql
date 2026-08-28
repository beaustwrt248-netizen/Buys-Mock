-- Admin configuration SECURITY DEFINER hardening.
-- Preserve the authenticated public RPC contract while removing direct caller
-- execution of the private privileged implementation.

revoke execute on function private.admin_set_config_impl(text, jsonb) from authenticated;
revoke execute on function private.admin_set_config_impl(text, jsonb) from public, anon;

create or replace function public.admin_set_config(config_key text, config_value jsonb)
returns void
language sql
security definer
set search_path = ''
as $$
  select private.admin_set_config_impl(config_key, config_value);
$$;

revoke all on function public.admin_set_config(text, jsonb) from public, anon;
grant execute on function public.admin_set_config(text, jsonb) to authenticated;

comment on function public.admin_set_config(text, jsonb) is
  'Authenticated Admin/Manager configuration RPC. SECURITY DEFINER wrapper delegates to a private implementation that performs explicit role checks, validation and audit logging; callers cannot execute the private implementation directly.';
