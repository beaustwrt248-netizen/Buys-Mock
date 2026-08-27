-- Admin Control 2.0 least-privilege hardening.
-- Keep the existing public admin_set_config RPC contract, validation and audit
-- behavior while moving the privileged SECURITY DEFINER implementation out of
-- the exposed public schema.

create or replace function private.admin_set_config_impl(config_key text, config_value jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  release_code integer;
  minimum_code integer;
  release_name text;
  minimum_name text;
  apk_url text;
  sha256 text;
  existing_release jsonb;
  existing_code integer;
begin
  if not private.is_admin_or_manager() then raise exception 'admin or manager required'; end if;
  if config_key not in ('feature_flags','current_release','minimum_supported_version') then raise exception 'unsupported config key'; end if;
  if jsonb_typeof(config_value) <> 'object' then raise exception 'config value must be a JSON object'; end if;

  if config_key = 'current_release' then
    begin release_code := (config_value->>'versionCode')::integer;
    exception when others then raise exception 'current release versionCode must be a positive integer'; end;
    release_name := btrim(coalesce(config_value->>'versionName',''));
    apk_url := btrim(coalesce(config_value->>'apkUrl',''));
    sha256 := lower(btrim(coalesce(config_value->>'sha256','')));
    if release_code < 1 or release_name = '' then raise exception 'current release identity is invalid'; end if;
    if apk_url not like 'https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/%' then raise exception 'current release APK URL is not trusted'; end if;
    if sha256 !~ '^[0-9a-f]{64}$' then raise exception 'current release SHA-256 is invalid'; end if;

    select value into existing_release from public.app_config where key='current_release' for update;
    if existing_release is not null then
      existing_code := nullif(existing_release->>'versionCode','')::integer;
      if existing_code is not null and release_code < existing_code then raise exception 'current release rollback is not allowed'; end if;
      if existing_code = release_code and (
        coalesce(existing_release->>'versionName','') <> release_name or
        coalesce(existing_release->>'apkUrl','') <> apk_url or
        lower(coalesce(existing_release->>'sha256','')) <> sha256
      ) then raise exception 'release identity cannot change for an existing versionCode'; end if;
    end if;

    select nullif(value->>'versionCode','')::integer into minimum_code from public.app_config where key='minimum_supported_version';
    if minimum_code is not null and minimum_code > release_code then raise exception 'current release cannot be below minimum supported version'; end if;
  elsif config_key = 'minimum_supported_version' then
    begin minimum_code := (config_value->>'versionCode')::integer;
    exception when others then raise exception 'minimum versionCode must be a positive integer'; end;
    minimum_name := btrim(coalesce(config_value->>'versionName',''));
    if minimum_code < 1 or minimum_name = '' then raise exception 'minimum supported version identity is invalid'; end if;
    select nullif(value->>'versionCode','')::integer into release_code from public.app_config where key='current_release';
    if release_code is null then raise exception 'current release is not configured'; end if;
    if minimum_code > release_code then raise exception 'minimum supported version cannot exceed current release'; end if;
    if config_value ? 'forceUpdate' and jsonb_typeof(config_value->'forceUpdate') <> 'boolean' then raise exception 'forceUpdate must be boolean'; end if;
  end if;

  insert into public.app_config(key,value,updated_by,updated_at)
  values(config_key,config_value,auth.uid(),now())
  on conflict(key) do update set value=excluded.value,updated_by=excluded.updated_by,updated_at=now();

  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(auth.uid(),'config_updated','config',config_key,jsonb_build_object('value',config_value));
end
$$;

revoke all on function private.admin_set_config_impl(text,jsonb) from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.admin_set_config_impl(text,jsonb) to authenticated;

create or replace function public.admin_set_config(config_key text, config_value jsonb)
returns void
language sql
security invoker
set search_path = ''
as $$
  select private.admin_set_config_impl(config_key, config_value);
$$;

revoke all on function public.admin_set_config(text,jsonb) from public, anon;
grant execute on function public.admin_set_config(text,jsonb) to authenticated;

comment on function public.admin_set_config(text,jsonb) is
  'Validated Admin/Manager configuration RPC. Public invoker wrapper delegates to a private SECURITY DEFINER implementation with explicit authorization and audit logging.';
