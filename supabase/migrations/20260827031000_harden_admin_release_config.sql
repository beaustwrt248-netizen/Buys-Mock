-- Production migration record for Admin Control / OTA release policy hardening.
-- Applied to Supabase on 2026-08-27 before being committed here.
--
-- Security intent:
--   * app_config is client-readable by authenticated installs, but not directly writable.
--   * Admin/manager configuration writes go through public.admin_set_config().
--   * Only known configuration keys are accepted.
--   * current_release metadata is validated and cannot roll versionCode backwards.
--   * minimum_supported_version cannot exceed current_release.
--   * release mutations remain audit logged.

revoke insert, update, delete on table public.app_config from authenticated;

drop policy if exists app_config_admin_insert on public.app_config;
drop policy if exists app_config_admin_update on public.app_config;
drop policy if exists app_config_admin_delete on public.app_config;

create or replace function public.admin_set_config(config_key text, config_value jsonb)
returns void
language plpgsql
security definer
set search_path = public, private
as $$
declare
  existing_value jsonb;
  release_code integer;
  minimum_code integer;
  version_name text;
  apk_url text;
  sha256 text;
begin
  if not private.is_admin_or_manager() then
    raise exception 'admin or manager required';
  end if;

  if config_key not in ('feature_flags', 'current_release', 'minimum_supported_version') then
    raise exception 'unsupported config key';
  end if;

  select value into existing_value from public.app_config where key = config_key;

  if config_key = 'current_release' then
    release_code := nullif(config_value->>'versionCode', '')::integer;
    version_name := btrim(coalesce(config_value->>'versionName', ''));
    apk_url := btrim(coalesce(config_value->>'apkUrl', ''));
    sha256 := lower(btrim(coalesce(config_value->>'sha256', '')));

    if release_code is null or release_code < 1 or version_name = '' then
      raise exception 'invalid release version';
    end if;
    if apk_url !~ '^https://github\.com/beaustwrt248-netizen/Buys-Mock/releases/download/v[0-9A-Za-z._-]+/B-and-L-Morley-[0-9A-Za-z._-]+\.apk$' then
      raise exception 'untrusted release APK URL';
    end if;
    if sha256 !~ '^[0-9a-f]{64}$' then
      raise exception 'invalid release SHA-256';
    end if;

    if existing_value is not null then
      if release_code < coalesce(nullif(existing_value->>'versionCode','')::integer, 0) then
        raise exception 'release rollback rejected';
      end if;
      if release_code = coalesce(nullif(existing_value->>'versionCode','')::integer, 0)
         and (coalesce(existing_value->>'versionName','') <> version_name
              or coalesce(existing_value->>'apkUrl','') <> apk_url
              or lower(coalesce(existing_value->>'sha256','')) <> sha256) then
        raise exception 'existing release versionCode metadata cannot be reassigned';
      end if;
    end if;

    select nullif(value->>'versionCode','')::integer into minimum_code
      from public.app_config where key = 'minimum_supported_version';
    if minimum_code is not null and minimum_code > release_code then
      raise exception 'current release cannot be below minimum supported version';
    end if;
  elsif config_key = 'minimum_supported_version' then
    minimum_code := nullif(config_value->>'versionCode','')::integer;
    version_name := btrim(coalesce(config_value->>'versionName',''));
    if minimum_code is null or minimum_code < 1 or version_name = '' then
      raise exception 'invalid minimum supported version';
    end if;
    select nullif(value->>'versionCode','')::integer into release_code
      from public.app_config where key = 'current_release';
    if release_code is null or minimum_code > release_code then
      raise exception 'minimum supported version cannot exceed current release';
    end if;
  elsif jsonb_typeof(config_value) <> 'object' then
    raise exception 'feature flags must be a JSON object';
  end if;

  insert into public.app_config(key, value, updated_by, updated_at)
  values(config_key, config_value, auth.uid(), now())
  on conflict(key) do update
    set value = excluded.value,
        updated_by = excluded.updated_by,
        updated_at = now();

  insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
  values(auth.uid(), 'config_updated', 'config', config_key, jsonb_build_object('value', config_value));
end
$$;

grant execute on function public.admin_set_config(text, jsonb) to authenticated;
