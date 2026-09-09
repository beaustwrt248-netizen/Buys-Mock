-- Accept privacy-minimal crash metadata from any enabled, authenticated Morley profile
-- without broadening direct INSERT or SELECT access to admin_error_events.

drop policy if exists admin_error_events_authenticated_insert on public.admin_error_events;

create or replace function public.report_client_crash(
  p_app_version text,
  p_device_model text,
  p_failing_screen text,
  p_error_class text,
  p_occurred_at timestamptz
)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    raise exception 'Authentication required' using errcode = '42501';
  end if;

  if not exists (
    select 1
    from public.profiles p
    where p.id = v_uid
      and p.is_enabled is true
  ) then
    raise exception 'Authorised profile required' using errcode = '42501';
  end if;

  if p_app_version is null or length(btrim(p_app_version)) not between 1 and 160
     or p_device_model is null or length(btrim(p_device_model)) not between 1 and 160
     or p_failing_screen is null or length(btrim(p_failing_screen)) not between 1 and 160
     or p_error_class is null or length(btrim(p_error_class)) not between 1 and 160 then
    raise exception 'Invalid crash metadata' using errcode = '22023';
  end if;

  if p_occurred_at is null
     or p_occurred_at < now() - interval '30 days'
     or p_occurred_at > now() + interval '5 minutes' then
    raise exception 'Invalid crash timestamp' using errcode = '22023';
  end if;

  insert into public.admin_error_events (
    app_version,
    device_model,
    failing_screen,
    error_class,
    occurred_at,
    received_at
  ) values (
    btrim(p_app_version),
    btrim(p_device_model),
    btrim(p_failing_screen),
    btrim(p_error_class),
    p_occurred_at,
    now()
  );
end;
$$;

revoke all on function public.report_client_crash(text, text, text, text, timestamptz) from public, anon;
grant execute on function public.report_client_crash(text, text, text, text, timestamptz) to authenticated;
