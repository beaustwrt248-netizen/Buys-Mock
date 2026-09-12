do $$
begin
  if has_function_privilege('anon', 'public.admin_revoke_user_sessions(uuid)', 'EXECUTE') then
    raise exception 'anon must not execute admin_revoke_user_sessions';
  end if;
  if has_function_privilege('authenticated', 'public.admin_revoke_user_sessions(uuid)', 'EXECUTE') then
    raise exception 'authenticated must not execute admin_revoke_user_sessions';
  end if;
  if not has_function_privilege('service_role', 'public.admin_revoke_user_sessions(uuid)', 'EXECUTE') then
    raise exception 'service_role must execute admin_revoke_user_sessions';
  end if;
end
$$;
