-- Approved #2033 least-privilege hardening.
--
-- This migration deliberately preserves the existing browser-required
-- SECURITY DEFINER model. It makes the reviewed EXECUTE ACLs explicit and
-- closes the one confirmed authorization gap: Guardian diagnostics now
-- require an enabled profile, not merely a non-null auth.uid().
--
-- It does not add browser table grants, RLS policies, invoker-mode
-- conversions, Auth configuration, pricing/recovery authority, or Guardian
-- repair/approval behavior.

do $$
declare
  v_signature text;
begin
  foreach v_signature in array array[
    'public.admin_create_download_invite(text,text,text,text,text,timestamptz)',
    'public.admin_create_team_invite(text,text,text,text,timestamptz)',
    'public.admin_inventory_create(text,text,numeric,numeric,bigint,uuid,text,text,text,text)',
    'public.admin_inventory_record_sale(uuid,numeric,numeric,numeric,text,text,timestamptz)',
    'public.admin_inventory_set_status(uuid,text)',
    'public.admin_reissue_team_invite(uuid,text,timestamptz)',
    'public.admin_revoke_download_invite(uuid)',
    'public.admin_revoke_team_invite(uuid)',
    'public.guardian_decide_incident(uuid,text)',
    'public.guardian_decide_repair(uuid,text)',
    'public.guardian_report_diagnostic(text,text,text,text,text,text,jsonb)',
    'public.guardian_set_agent_controls(boolean,boolean,boolean,text)',
    'public.guardian_set_controls(boolean,boolean,text,text,boolean,boolean,numeric,integer,boolean,boolean,text)'
  ]
  loop
    if to_regprocedure(v_signature) is null then
      raise exception 'required reviewed RPC is missing: %', v_signature;
    end if;
  end loop;
end
$$;

create or replace function public.guardian_report_diagnostic(
  p_kind text,
  p_message text,
  p_severity text default 'error'::text,
  p_app_version text default null::text,
  p_route text default null::text,
  p_fingerprint text default null::text,
  p_metadata jsonb default '{}'::jsonb
)
returns uuid
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private'
as $function$
declare
  v_id uuid;
  v_kind text := left(coalesce(nullif(trim(p_kind),''),'runtime_error'),80);
  v_message text := left(coalesce(nullif(trim(p_message),''),'Unknown runtime error'),1200);
  v_severity text := lower(coalesce(nullif(trim(p_severity),''),'error'));
  v_fingerprint text;
  v_normalized_message text;
  v_normalized_route text;
  v_meta jsonb := '{}'::jsonb;
  v_enabled boolean;
  kv record;
begin
  if auth.uid() is null or not exists (
    select 1
    from public.profiles p
    where p.id = auth.uid()
      and p.is_enabled = true
  ) then
    raise exception 'Authentication required';
  end if;

  select enabled and not kill_switch into v_enabled
  from public.guardian_settings where singleton = true;
  if not coalesce(v_enabled,false) then return null; end if;

  if v_severity not in ('info','warning','error','critical') then v_severity := 'error'; end if;

  if jsonb_typeof(coalesce(p_metadata,'{}'::jsonb)) = 'object' then
    for kv in select key, value from jsonb_each(coalesce(p_metadata,'{}'::jsonb)) loop
      if kv.key !~* '(token|secret|password|authorization|cookie|session|key|credential)' then
        v_meta := v_meta || jsonb_build_object(left(kv.key,64),
          case when jsonb_typeof(kv.value) = 'string' then to_jsonb(left(trim(both '"' from kv.value::text),500)) else kv.value end);
      end if;
    end loop;
  end if;

  -- Browser srcdoc routes include volatile _update query values. They identify the same
  -- generated runtime and must not affect incident identity. Normalize both the route
  -- and the source embedded in the message. Server-side identity is authoritative for
  -- runtime JS diagnostics; a caller-supplied hash is telemetry only.
  v_normalized_route := regexp_replace(coalesce(trim(p_route),''), '\?.*$', '');
  v_normalized_message := regexp_replace(v_message, 'about:srcdoc\?[^:@[:space:]]*', 'about:srcdoc', 'gi');
  v_fingerprint := left('srv-' || md5(v_kind || '|' || v_normalized_message || '|' || v_normalized_route),128);

  -- Reuse any unresolved incident regardless of age. A failed repair is still the same
  -- unresolved bug; a new incident would only make the approval queue chase duplicates.
  select id into v_id
  from public.guardian_incidents
  where fingerprint = v_fingerprint
    and state not in ('resolved','ignored')
  order by
    case state when 'awaiting_approval' then 0 when 'applying' then 1 when 'proposed' then 2 when 'diagnosing' then 3 when 'queued' then 4 else 5 end,
    last_seen_at desc
  limit 1
  for update;

  if v_id is not null then
    update public.guardian_incidents
    set occurrence_count = occurrence_count + 1,
        last_seen_at = now(),
        app_version = coalesce(left(p_app_version,80),app_version),
        route = coalesce(left(p_route,300),route),
        diagnostic_message = v_message,
        diagnostic_metadata = diagnostic_metadata || v_meta,
        risk_level = case
          when v_severity = 'critical' then 'critical'
          when risk_level in ('critical','high') then risk_level
          when v_severity = 'error' and risk_level = 'low' then 'medium'
          else risk_level
        end,
        updated_at = now()
    where id = v_id;
    return v_id;
  end if;

  -- Do not reopen a terminal incident from cached telemetry emitted by the same build.
  -- A later build/version reporting the same canonical error is a genuine recurrence
  -- and will create a fresh incident for review.
  select id into v_id
  from public.guardian_incidents
  where fingerprint = v_fingerprint
    and state in ('resolved','ignored')
    and (
      (nullif(trim(p_app_version),'') is not null and app_version = left(p_app_version,80))
      or (nullif(trim(p_app_version),'') is null and updated_at > now() - interval '30 minutes')
    )
  order by updated_at desc
  limit 1
  for update;

  if v_id is not null then
    update public.guardian_incidents
    set occurrence_count = occurrence_count + 1,
        last_seen_at = now(),
        diagnostic_metadata = diagnostic_metadata || v_meta,
        updated_at = now()
    where id = v_id;
    return v_id;
  end if;

  insert into public.guardian_incidents(
    source,state,risk_level,classification,auto_fix_eligible,requires_approval,
    fingerprint,occurrence_count,first_seen_at,last_seen_at,app_version,route,
    diagnostic_kind,diagnostic_message,diagnostic_metadata,diagnosis_summary
  ) values (
    'runtime_crash','queued',
    case v_severity when 'critical' then 'critical' when 'warning' then 'low' when 'info' then 'low' else 'medium' end,
    v_kind,false,true,
    v_fingerprint,1,now(),now(),left(p_app_version,80),left(p_route,300),
    v_kind,v_message,v_meta,
    left('Automatically captured by Morley Guardian bug checker: ' || v_message,2000)
  ) returning id into v_id;

  return v_id;
end;
$function$;

revoke execute on function public.admin_create_download_invite(text,text,text,text,text,timestamptz) from public, anon;
grant execute on function public.admin_create_download_invite(text,text,text,text,text,timestamptz) to authenticated;
revoke execute on function public.admin_create_team_invite(text,text,text,text,timestamptz) from public, anon;
grant execute on function public.admin_create_team_invite(text,text,text,text,timestamptz) to authenticated;
revoke execute on function public.admin_inventory_create(text,text,numeric,numeric,bigint,uuid,text,text,text,text) from public, anon;
grant execute on function public.admin_inventory_create(text,text,numeric,numeric,bigint,uuid,text,text,text,text) to authenticated;
revoke execute on function public.admin_inventory_record_sale(uuid,numeric,numeric,numeric,text,text,timestamptz) from public, anon;
grant execute on function public.admin_inventory_record_sale(uuid,numeric,numeric,numeric,text,text,timestamptz) to authenticated;
revoke execute on function public.admin_inventory_set_status(uuid,text) from public, anon;
grant execute on function public.admin_inventory_set_status(uuid,text) to authenticated;
revoke execute on function public.admin_reissue_team_invite(uuid,text,timestamptz) from public, anon;
grant execute on function public.admin_reissue_team_invite(uuid,text,timestamptz) to authenticated;
revoke execute on function public.admin_revoke_download_invite(uuid) from public, anon;
grant execute on function public.admin_revoke_download_invite(uuid) to authenticated;
revoke execute on function public.admin_revoke_team_invite(uuid) from public, anon;
grant execute on function public.admin_revoke_team_invite(uuid) to authenticated;
revoke execute on function public.guardian_decide_incident(uuid,text) from public, anon;
grant execute on function public.guardian_decide_incident(uuid,text) to authenticated;
revoke execute on function public.guardian_decide_repair(uuid,text) from public, anon;
grant execute on function public.guardian_decide_repair(uuid,text) to authenticated;
revoke execute on function public.guardian_report_diagnostic(text,text,text,text,text,text,jsonb) from public, anon;
grant execute on function public.guardian_report_diagnostic(text,text,text,text,text,text,jsonb) to authenticated;
revoke execute on function public.guardian_set_agent_controls(boolean,boolean,boolean,text) from public, anon;
grant execute on function public.guardian_set_agent_controls(boolean,boolean,boolean,text) to authenticated;
revoke execute on function public.guardian_set_controls(boolean,boolean,text,text,boolean,boolean,numeric,integer,boolean,boolean,text) from public, anon;
grant execute on function public.guardian_set_controls(boolean,boolean,text,text,boolean,boolean,numeric,integer,boolean,boolean,text) to authenticated;
