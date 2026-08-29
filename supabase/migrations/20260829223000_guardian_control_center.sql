-- Guardian Control Center: backend-enforced operating modes, kill switch and guarded self-evolution controls.

alter table public.guardian_settings
  add column if not exists kill_switch boolean not null default false,
  add column if not exists kill_switch_reason text,
  add column if not exists operating_mode text not null default 'observe',
  add column if not exists learning_enabled boolean not null default true,
  add column if not exists evolution_enabled boolean not null default false,
  add column if not exists confidence_threshold numeric(4,3) not null default 0.850,
  add column if not exists max_parallel_repairs integer not null default 1,
  add column if not exists quarantine_on_repeated_failure boolean not null default true;

alter table public.guardian_settings drop constraint if exists guardian_settings_operating_mode_check;
alter table public.guardian_settings add constraint guardian_settings_operating_mode_check
  check (operating_mode in ('observe','assist','guarded_auto'));

alter table public.guardian_settings drop constraint if exists guardian_settings_confidence_threshold_check;
alter table public.guardian_settings add constraint guardian_settings_confidence_threshold_check
  check (confidence_threshold >= 0.500 and confidence_threshold <= 0.999);

alter table public.guardian_settings drop constraint if exists guardian_settings_parallel_repairs_check;
alter table public.guardian_settings add constraint guardian_settings_parallel_repairs_check
  check (max_parallel_repairs between 1 and 5);

alter table public.guardian_settings drop constraint if exists guardian_settings_kill_switch_check;
alter table public.guardian_settings add constraint guardian_settings_kill_switch_check
  check (not kill_switch or (enabled = false and auto_fix_enabled = false));

update public.guardian_settings
set require_human_for_code = true
where singleton = true;

alter table public.guardian_settings drop constraint if exists guardian_settings_human_code_required;
alter table public.guardian_settings add constraint guardian_settings_human_code_required
  check (require_human_for_code = true);

-- Settings changes must go through the guarded RPC rather than direct table updates.
revoke update on table public.guardian_settings from authenticated;

drop policy if exists guardian_settings_admin_update on public.guardian_settings;

create or replace function public.guardian_set_controls(
  p_enabled boolean,
  p_auto_fix_enabled boolean,
  p_max_auto_risk text,
  p_operating_mode text,
  p_learning_enabled boolean,
  p_evolution_enabled boolean,
  p_confidence_threshold numeric,
  p_max_parallel_repairs integer,
  p_quarantine_on_repeated_failure boolean,
  p_kill_switch boolean,
  p_kill_switch_reason text default null
)
returns public.guardian_settings
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  v_role text;
  v_current public.guardian_settings;
  v_result public.guardian_settings;
begin
  select role into v_role from public.profiles where id = auth.uid() and is_enabled = true;
  if v_role not in ('admin','manager') then
    raise exception 'Guardian controls require Admin or Manager access';
  end if;

  select * into v_current from public.guardian_settings where singleton = true for update;

  if v_current.kill_switch and not p_kill_switch and v_role <> 'admin' then
    raise exception 'Only an Admin can disengage the Guardian kill switch';
  end if;

  if p_max_auto_risk not in ('low','medium') then
    raise exception 'Invalid maximum automatic risk';
  end if;
  if p_operating_mode not in ('observe','assist','guarded_auto') then
    raise exception 'Invalid Guardian operating mode';
  end if;
  if p_confidence_threshold < 0.500 or p_confidence_threshold > 0.999 then
    raise exception 'Confidence threshold must be between 0.500 and 0.999';
  end if;
  if p_max_parallel_repairs < 1 or p_max_parallel_repairs > 5 then
    raise exception 'Parallel repair limit must be between 1 and 5';
  end if;
  if p_kill_switch and coalesce(length(trim(p_kill_switch_reason)),0) < 3 then
    raise exception 'A kill switch reason is required';
  end if;

  update public.guardian_settings
  set enabled = case when p_kill_switch then false else p_enabled end,
      auto_fix_enabled = case when p_kill_switch then false else p_auto_fix_enabled end,
      max_auto_risk = p_max_auto_risk,
      operating_mode = p_operating_mode,
      learning_enabled = p_learning_enabled,
      evolution_enabled = p_evolution_enabled,
      confidence_threshold = p_confidence_threshold,
      max_parallel_repairs = p_max_parallel_repairs,
      quarantine_on_repeated_failure = p_quarantine_on_repeated_failure,
      kill_switch = p_kill_switch,
      kill_switch_reason = case when p_kill_switch then left(trim(p_kill_switch_reason),500) else null end,
      require_human_for_code = true,
      updated_by = auth.uid(),
      updated_at = now()
  where singleton = true
  returning * into v_result;

  insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
  values(
    auth.uid(),
    case
      when not v_current.kill_switch and v_result.kill_switch then 'guardian_kill_switch_engaged'
      when v_current.kill_switch and not v_result.kill_switch then 'guardian_kill_switch_disengaged'
      else 'guardian_controls_updated'
    end,
    'guardian_settings',
    'singleton',
    jsonb_build_object(
      'enabled', v_result.enabled,
      'auto_fix_enabled', v_result.auto_fix_enabled,
      'max_auto_risk', v_result.max_auto_risk,
      'operating_mode', v_result.operating_mode,
      'learning_enabled', v_result.learning_enabled,
      'evolution_enabled', v_result.evolution_enabled,
      'confidence_threshold', v_result.confidence_threshold,
      'max_parallel_repairs', v_result.max_parallel_repairs,
      'quarantine_on_repeated_failure', v_result.quarantine_on_repeated_failure,
      'kill_switch', v_result.kill_switch,
      'kill_switch_reason', v_result.kill_switch_reason,
      'require_human_for_code', true
    )
  );

  return v_result;
end;
$$;

revoke all on function public.guardian_set_controls(boolean,boolean,text,text,boolean,boolean,numeric,integer,boolean,boolean,text) from public;
grant execute on function public.guardian_set_controls(boolean,boolean,text,text,boolean,boolean,numeric,integer,boolean,boolean,text) to authenticated;

-- New reports must not enter Guardian while the kill switch is engaged.
create or replace function private.guardian_queue_support_ticket()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
begin
  if coalesce((select enabled and not kill_switch from public.guardian_settings where singleton = true), false) then
    insert into public.guardian_incidents(ticket_id, source, state, risk_level, classification, auto_fix_eligible, requires_approval)
    values(new.id, 'report_problem', 'queued', case when new.priority = 'urgent' then 'high' else 'medium' end, new.category, false, true)
    on conflict (ticket_id) do nothing;
  end if;
  return new;
end;
$$;
revoke all on function private.guardian_queue_support_ticket() from public;

-- Preserve the existing audit trigger but include Control Center fields.
create or replace function private.guardian_audit_change()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  if tg_table_name = 'guardian_settings' then
    -- guardian_set_controls writes the durable settings audit entry itself.
    return new;
  end if;
  insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
  values(auth.uid(), 'guardian_incident_updated', 'guardian_incident', new.id::text,
    jsonb_build_object('state',new.state,'risk_level',new.risk_level,'requires_approval',new.requires_approval));
  return new;
end;
$$;
revoke all on function private.guardian_audit_change() from public;
