-- Remove unnecessary SECURITY DEFINER privileges from the authenticated device-registration RPC.
-- RLS on public.devices already restricts writes to auth.uid(), and authenticated callers
-- have the required table privileges. Keep the public RPC surface, but execute as caller.

create or replace function public.register_device_public(
  p_installation_id text,
  p_device_name text,
  p_app_version text,
  p_app_version_code integer,
  p_fcm_token text,
  p_notifications_enabled boolean
)
returns uuid
language plpgsql
security invoker
set search_path = ''
as $function$
declare
  v_user uuid := auth.uid();
  v_id uuid;
begin
  if v_user is null then
    raise exception 'Authentication required';
  end if;

  if not exists (
    select 1
    from public.profiles
    where id = v_user
      and is_enabled = true
  ) then
    raise exception 'Account is not authorised';
  end if;

  insert into public.devices(
    user_id,
    installation_id,
    platform,
    device_name,
    app_version,
    app_version_code,
    fcm_token,
    notifications_enabled,
    last_seen_at,
    updated_at
  )
  values (
    v_user,
    p_installation_id,
    'android',
    p_device_name,
    p_app_version,
    p_app_version_code,
    p_fcm_token,
    p_notifications_enabled,
    now(),
    now()
  )
  on conflict (installation_id) do update set
    user_id = v_user,
    device_name = excluded.device_name,
    app_version = excluded.app_version,
    app_version_code = excluded.app_version_code,
    fcm_token = excluded.fcm_token,
    notifications_enabled = excluded.notifications_enabled,
    last_seen_at = now(),
    updated_at = now()
  returning id into v_id;

  return v_id;
end;
$function$;

revoke all on function public.register_device_public(text, text, text, integer, text, boolean) from public;
grant execute on function public.register_device_public(text, text, text, integer, text, boolean) to authenticated;
