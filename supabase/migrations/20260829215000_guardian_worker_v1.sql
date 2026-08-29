-- Guardian Worker v1: worker metadata, existing-ticket backfill, approval RPC and event history.

alter table public.guardian_incidents
  add column if not exists worker_version text,
  add column if not exists reproduction_summary text,
  add column if not exists test_plan text,
  add column if not exists resolution_summary text,
  add column if not exists last_worker_at timestamptz;

create table if not exists public.guardian_incident_events (
  id bigint generated always as identity primary key,
  incident_id uuid not null references public.guardian_incidents(id) on delete cascade,
  event_type text not null,
  actor_user_id uuid references auth.users(id) on delete set null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists guardian_incident_events_incident_created_idx
  on public.guardian_incident_events(incident_id, created_at desc);

alter table public.guardian_incident_events enable row level security;
revoke all on table public.guardian_incident_events from anon, authenticated;
grant select on table public.guardian_incident_events to authenticated;

drop policy if exists guardian_incident_events_admin_read on public.guardian_incident_events;
create policy guardian_incident_events_admin_read on public.guardian_incident_events
for select to authenticated using (private.is_admin_or_manager());

create or replace function private.guardian_log_incident_transition()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public
as $$
begin
  if new.state is distinct from old.state
     or new.diagnosis_summary is distinct from old.diagnosis_summary
     or new.github_pr_number is distinct from old.github_pr_number
     or new.last_error_code is distinct from old.last_error_code then
    insert into public.guardian_incident_events(incident_id,event_type,actor_user_id,details)
    values(
      new.id,
      case when new.state is distinct from old.state then 'state_changed' else 'worker_updated' end,
      auth.uid(),
      jsonb_build_object(
        'from_state',old.state,
        'to_state',new.state,
        'risk_level',new.risk_level,
        'classification',new.classification,
        'github_pr_number',new.github_pr_number,
        'last_error_code',new.last_error_code
      )
    );
  end if;
  return new;
end;
$$;
revoke all on function private.guardian_log_incident_transition() from public;

drop trigger if exists trg_guardian_incident_transition on public.guardian_incidents;
create trigger trg_guardian_incident_transition
after update on public.guardian_incidents
for each row execute function private.guardian_log_incident_transition();

-- Bring unresolved reports created before Guardian Core into the queue once.
insert into public.guardian_incidents(ticket_id,source,state,risk_level,classification,auto_fix_eligible,requires_approval)
select t.id,
       'report_problem',
       'queued',
       case when t.priority='urgent' then 'high' else 'medium' end,
       t.category,
       false,
       true
from public.support_tickets t
where t.status in ('open','in_progress','waiting_on_user')
  and not exists (select 1 from public.guardian_incidents g where g.ticket_id=t.id)
on conflict (ticket_id) do nothing;

create or replace function public.guardian_decide_incident(
  incident_id uuid,
  decision text
)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  target public.guardian_incidents%rowtype;
begin
  if not private.is_admin_or_manager() then
    raise exception 'Admin or Manager access required';
  end if;
  if decision not in ('approve','reject','retry') then
    raise exception 'Invalid Guardian decision';
  end if;

  select * into target from public.guardian_incidents where id=incident_id for update;
  if not found then raise exception 'Guardian incident not found'; end if;

  if decision='approve' then
    if target.state not in ('proposed','awaiting_approval') then
      raise exception 'Incident is not awaiting approval';
    end if;
    update public.guardian_incidents
      set state='applying', approved_by=auth.uid(), approved_at=now(), last_error_code=null
      where id=incident_id;
  elsif decision='reject' then
    update public.guardian_incidents
      set state='ignored', approved_by=auth.uid(), approved_at=now()
      where id=incident_id;
  else
    update public.guardian_incidents
      set state='queued', last_error_code=null, attempt_count=attempt_count+1
      where id=incident_id;
  end if;
end;
$$;

revoke all on function public.guardian_decide_incident(uuid,text) from public, anon;
grant execute on function public.guardian_decide_incident(uuid,text) to authenticated;
