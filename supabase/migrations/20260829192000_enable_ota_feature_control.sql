-- Admin Control 2.0: explicit OTA enable/disable feature control.
-- Preserves all unrelated feature flags and keeps release identity validation intact.

update public.app_config
set value = jsonb_set(value, '{otaEnabled}', 'true'::jsonb, true),
    updated_at = now()
where key = 'feature_flags'
  and jsonb_typeof(value) = 'object'
  and not (value ? 'otaEnabled');

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
  existing_flags jsonb;
  maintenance_message text;
begin
  if not private.is_admin_or_manager() then raise exception 'admin or manager required'; end if;
  if config_key not in ('feature_flags','current_release','minimum_supported_version') then raise exception 'unsupported config key'; end if;
  if jsonb_typeof(config_value) <> 'object' then raise exception 'config value must be a JSON object'; end if;

  if config_key = 'feature_flags' then
    select value into existing_flags from public.app_config where key='feature_flags' for update;
    if existing_flags is null or jsonb_typeof(existing_flags) <> 'object' then
      raise exception 'existing feature flags are not configured';
    end if;
    if not (config_value ? 'maintenanceMode') or jsonb_typeof(config_value->'maintenanceMode') <> 'boolean' then
      raise exception 'maintenanceMode must be boolean';
    end if;
    if not (config_value ? 'otaEnabled') or jsonb_typeof(config_value->'otaEnabled') <> 'boolean' then
      raise exception 'otaEnabled must be boolean';
    end if;
    maintenance_message := coalesce(config_value->>'maintenanceMessage','');
    if length(maintenance_message) > 160 then raise exception 'maintenanceMessage is too long'; end if;
    if (config_value - 'maintenanceMode' - 'maintenanceMessage' - 'otaEnabled')
       <> (existing_flags - 'maintenanceMode' - 'maintenanceMessage' - 'otaEnabled') then
      raise exception 'only maintenanceMode, maintenanceMessage and otaEnabled may be changed remotely';
    end if;
    config_value := jsonb_set(config_value, '{maintenanceMessage}', to_jsonb(maintenance_message), true);
  elsif config_key = 'current_release' then
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
  values(auth.uid(),'config_updated','config',config_key,
    case when config_key='feature_flags'
      then jsonb_build_object(
        'maintenanceMode',config_value->'maintenanceMode',
        'maintenanceMessage',config_value->'maintenanceMessage',
        'otaEnabled',config_value->'otaEnabled'
      )
      else jsonb_build_object('value',config_value)
    end);
end
$$;

revoke all on function private.admin_set_config_impl(text,jsonb) from public, anon;
grant usage on schema private to authenticated;
grant execute on function private.admin_set_config_impl(text,jsonb) to authenticated;

comment on function private.admin_set_config_impl(text,jsonb) is
  'Privileged Admin/Manager config implementation. feature_flags changes are limited to maintenanceMode/message and otaEnabled; all changes are audited.';
