do $$
declare
  fn_source text;
begin
  select pg_get_functiondef('public.admin_revoke_user_sessions(uuid)'::regprocedure) into fn_source;
  if position('delete from auth.sessions' in lower(fn_source)) = 0 then
    raise exception 'admin_revoke_user_sessions must revoke auth sessions';
  end if;
end
$$;
