-- Guardian bug checker: authenticated, deduplicated runtime diagnostics.
-- Diagnostic intake is telemetry only. It cannot merge, deploy, mutate auth/RLS/secrets,
-- or perform destructive database actions.

alter table public.guardian_incidents
  add column if not exists fingerprint text,
  add column if not exists occurrence_count integer not null default 1,
  add column if not exists first_seen_at timestamptz not null default now(),
  add column if not exists last_seen_at timestamptz not null default now(),
  add column if not exists app_version text,
  add column if not exists route text,
  add column if not exists diagnostic_kind text,
  add column if not exists diagnostic_message text,
  add column if not exists diagnostic_metadata jsonb not null default '{}'::jsonb;

alter table public.guardian_incidents drop constraint if exists guardian_incidents_occurrence_count_check;
alter table public.guardian_incidents add constraint guardian_incidents_occurrence_count_check check (occurrence_count >= 1);
create index if not exists guardian_incidents_fingerprint_seen_idx on public.guardian_incidents(fingerprint, last_seen_at desc);

create or replace function public.guardian_report_diagnostic(
  p_kind text,
  p_message text,
  p_severity text default 'error',
  p_app_version text default null,
  p_route text default null,
  p_fingerprint text default null,
  p_metadata jsonb default '{}'::jsonb
)
returns uuid
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  v_id uuid;
  v_kind text := left(coalesce(nullif(trim(p_kind),''),'runtime_error'),80);
  v_message text := left(coalesce(nullif(trim(p_message),''),'Unknown runtime error'),1200);
  v_severity text := lower(coalesce(nullif(trim(p_severity),''),'error'));
  v_fingerprint text;
  v_meta jsonb := '{}'::jsonb;
  v_enabled boolean;
  kv record;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;

  select enabled and not kill_switch into v_enabled
  from public.guardian_settings where singleton = true;
  if not coalesce(v_enabled,false) then return null; end if;

  if v_severity not in ('info','warning','error','critical') then v_severity := 'error'; end if;

  -- Only accept shallow metadata keys that cannot plausibly contain credentials.
  if jsonb_typeof(coalesce(p_metadata,'{}'::jsonb)) = 'object' then
    for kv in select key, value from jsonb_each(coalesce(p_metadata,'{}'::jsonb)) loop
      if kv.key !~* '(token|secret|password|authorization|cookie|session|key|credential)' then
        v_meta := v_meta || jsonb_build_object(left(kv.key,64),
          case when jsonb_typeof(kv.value) = 'string' then to_jsonb(left(trim(both '"' from kv.value::text),500)) else kv.value end);
      end if;
    end loop;
  end if;

  v_fingerprint := left(coalesce(nullif(trim(p_fingerprint),''), md5(v_kind || '|' || v_message || '|' || coalesce(p_route,''))),128);

  select id into v_id
  from public.guardian_incidents
  where fingerprint = v_fingerprint
    and state not in ('resolved','ignored')
    and last_seen_at > now() - interval '24 hours'
  order by last_seen_at desc
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
$$;

revoke all on function public.guardian_report_diagnostic(text,text,text,text,text,text,jsonb) from public;
grant execute on function public.guardian_report_diagnostic(text,text,text,text,text,text,jsonb) to authenticated;
