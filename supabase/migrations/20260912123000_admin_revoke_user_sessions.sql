create or replace function public.admin_revoke_user_sessions(target_user_id uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  revoked_sessions integer := 0;
begin
  delete from auth.sessions
  where user_id = target_user_id;

  get diagnostics revoked_sessions = row_count;
  return revoked_sessions;
end;
$$;

revoke all on function public.admin_revoke_user_sessions(uuid) from public;
revoke all on function public.admin_revoke_user_sessions(uuid) from anon;
revoke all on function public.admin_revoke_user_sessions(uuid) from authenticated;
grant execute on function public.admin_revoke_user_sessions(uuid) to service_role;

comment on function public.admin_revoke_user_sessions(uuid) is
  'Service-role-only helper used by admin Edge Functions to revoke all auth sessions for one user. Deleting auth.sessions cascades to refresh_tokens; existing access JWTs remain valid until expiry, matching Supabase global sign-out semantics.';
