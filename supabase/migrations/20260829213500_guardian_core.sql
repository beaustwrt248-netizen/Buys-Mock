-- Morley Guardian core: protected incident queue fed by Report a Problem.
-- This intentionally does not grant autonomous production code/database mutation.

create table if not exists public.guardian_settings (
  singleton boolean primary key default true check (singleton),
  enabled boolean not null default true,
  auto_fix_enabled boolean not null default false,
  max_auto_risk text not null default 'low' check (max_auto_risk in ('low','medium')),
  require_human_for_code boolean not null default true,
  updated_by uuid references auth.users(id) on delete set null,
  updated_at timestamptz not null default now()
);

insert into public.guardian_settings(singleton, enabled, auto_fix_enabled, max_auto_risk, require_human_for_code)
values (true, true, false, 'low', true)
on conflict (singleton) do nothing;

create table if not exists public.guardian_incidents (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid unique references public.support_tickets(id) on delete cascade,
  source text not null check (source in ('report_problem','admin_crash','runtime_crash','manual')),
  state text not null default 'queued' check (state in ('queued','diagnosing','proposed','awaiting_approval','applying','verifying','resolved','failed','ignored')),
  risk_level text not null default 'medium' check (risk_level in ('low','medium','high','critical')),
  classification text,
  confidence numeric(4,3) check (confidence is null or (confidence >= 0 and confidence <= 1)),
  diagnosis_summary text check (diagnosis_summary is null or char_length(diagnosis_summary) <= 2000),
  proposed_action text check (proposed_action is null or char_length(proposed_action) <= 4000),
  auto_fix_eligible boolean not null default false,
  requires_approval boolean not null default true,
  attempt_count integer not null default 0 check (attempt_count >= 0),
  github_branch text,
  github_pr_number integer,
  last_error_code text,
  approved_by uuid references auth.users(id) on delete set null,
  approved_at timestamptz,
  applied_at timestamptz,
  verified_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists guardian_incidents_state_created_idx
  on public.guardian_incidents(state, created_at desc);
create index if not exists guardian_incidents_risk_created_idx
  on public.guardian_incidents(risk_level, created_at desc);

alter table public.guardian_settings enable row level security;
alter table public.guardian_incidents enable row level security;

revoke all on table public.guardian_settings from anon, authenticated;
revoke all on table public.guardian_incidents from anon, authenticated;
grant select, update on table public.guardian_settings to authenticated;
grant select, update on table public.guardian_incidents to authenticated;

drop policy if exists guardian_settings_admin_read on public.guardian_settings;
create policy guardian_settings_admin_read on public.guardian_settings
for select to authenticated using (private.is_admin_or_manager());

drop policy if exists guardian_settings_admin_update on public.guardian_settings;
create policy guardian_settings_admin_update on public.guardian_settings
for update to authenticated
using (private.is_admin_or_manager())
with check (private.is_admin_or_manager());

drop policy if exists guardian_incidents_admin_read on public.guardian_incidents;
create policy guardian_incidents_admin_read on public.guardian_incidents
for select to authenticated using (private.is_admin_or_manager());

drop policy if exists guardian_incidents_admin_update on public.guardian_incidents;
create policy guardian_incidents_admin_update on public.guardian_incidents
for update to authenticated
using (private.is_admin_or_manager())
with check (private.is_admin_or_manager());

create or replace function private.guardian_queue_support_ticket()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
begin
  if coalesce((select enabled from public.guardian_settings where singleton = true), false) then
    insert into public.guardian_incidents(
      ticket_id, source, state, risk_level, classification, auto_fix_eligible, requires_approval
    ) values (
      new.id, 'report_problem', 'queued',
      case when new.priority = 'urgent' then 'high' else 'medium' end,
      new.category, false, true
    )
    on conflict (ticket_id) do nothing;
  end if;
  return new;
end;
$$;

revoke all on function private.guardian_queue_support_ticket() from public;

drop trigger if exists trg_guardian_queue_support_ticket on public.support_tickets;
create trigger trg_guardian_queue_support_ticket
after insert on public.support_tickets
for each row execute function private.guardian_queue_support_ticket();

create or replace function private.guardian_touch_row()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

revoke all on function private.guardian_touch_row() from public;

drop trigger if exists trg_guardian_touch_settings on public.guardian_settings;
create trigger trg_guardian_touch_settings before update on public.guardian_settings
for each row execute function private.guardian_touch_row();

drop trigger if exists trg_guardian_touch_incident on public.guardian_incidents;
create trigger trg_guardian_touch_incident before update on public.guardian_incidents
for each row execute function private.guardian_touch_row();

create or replace function private.guardian_audit_change()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  if tg_table_name = 'guardian_settings' then
    insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
    values (
      auth.uid(), 'guardian_settings_updated', 'guardian_settings', 'singleton',
      jsonb_build_object(
        'enabled', new.enabled,
        'auto_fix_enabled', new.auto_fix_enabled,
        'max_auto_risk', new.max_auto_risk,
        'require_human_for_code', new.require_human_for_code
      )
    );
  else
    insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
    values (
      auth.uid(), 'guardian_incident_updated', 'guardian_incident', new.id::text,
      jsonb_build_object('state', new.state, 'risk_level', new.risk_level, 'requires_approval', new.requires_approval)
    );
  end if;
  return new;
end;
$$;

revoke all on function private.guardian_audit_change() from public;

drop trigger if exists trg_guardian_audit_settings on public.guardian_settings;
create trigger trg_guardian_audit_settings after update on public.guardian_settings
for each row execute function private.guardian_audit_change();

drop trigger if exists trg_guardian_audit_incident on public.guardian_incidents;
create trigger trg_guardian_audit_incident after update on public.guardian_incidents
for each row execute function private.guardian_audit_change();
