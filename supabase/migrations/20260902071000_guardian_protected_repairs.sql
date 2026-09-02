-- Guardian protected repair jobs: durable candidate generation and approval evidence.

create table if not exists public.guardian_repairs (
  id uuid primary key default gen_random_uuid(),
  incident_id uuid not null unique references public.guardian_incidents(id) on delete cascade,
  status text not null default 'requested',
  requested_by uuid references auth.users(id),
  requested_at timestamptz not null default now(),
  base_ref text not null default 'main',
  branch_name text,
  candidate_files jsonb not null default '[]'::jsonb,
  patch_summary text,
  patch_text text,
  test_plan jsonb not null default '[]'::jsonb,
  test_results jsonb not null default '[]'::jsonb,
  github_pr_number integer,
  github_pr_url text,
  last_error_code text,
  generated_at timestamptz,
  tested_at timestamptz,
  completed_at timestamptz,
  updated_at timestamptz not null default now(),
  constraint guardian_repairs_status_check check (status in (
    'requested','generating','candidate_ready','testing','tests_passed','tests_failed',
    'awaiting_merge_approval','merged','cancelled','failed'
  )),
  constraint guardian_repairs_candidate_files_array check (jsonb_typeof(candidate_files) = 'array'),
  constraint guardian_repairs_test_plan_array check (jsonb_typeof(test_plan) = 'array'),
  constraint guardian_repairs_test_results_array check (jsonb_typeof(test_results) = 'array')
);

create index if not exists guardian_repairs_status_requested_idx
  on public.guardian_repairs(status, requested_at desc);

alter table public.guardian_repairs enable row level security;

revoke all on table public.guardian_repairs from anon;
revoke insert, update, delete on table public.guardian_repairs from authenticated;
grant select on table public.guardian_repairs to authenticated;

drop policy if exists guardian_repairs_admin_read on public.guardian_repairs;
create policy guardian_repairs_admin_read
  on public.guardian_repairs
  for select
  to authenticated
  using (private.is_admin_or_manager());

-- Approval creates exactly one protected repair request. The worker may generate a
-- candidate, but code merges/deployments remain outside this RPC and require a
-- separate human-controlled step.
create or replace function public.guardian_decide_incident(incident_id uuid, decision text)
returns void
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  target public.guardian_incidents%rowtype;
  repair_id uuid;
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

    insert into public.guardian_repairs(incident_id, requested_by, status, base_ref)
    values(incident_id, auth.uid(), 'requested', 'main')
    on conflict (incident_id) do update
      set status = case
        when public.guardian_repairs.status in ('failed','cancelled','tests_failed') then 'requested'
        else public.guardian_repairs.status
      end,
      requested_by = auth.uid(),
      requested_at = case
        when public.guardian_repairs.status in ('failed','cancelled','tests_failed') then now()
        else public.guardian_repairs.requested_at
      end,
      last_error_code = case
        when public.guardian_repairs.status in ('failed','cancelled','tests_failed') then null
        else public.guardian_repairs.last_error_code
      end,
      updated_at = now()
    returning id into repair_id;

    update public.guardian_incidents
      set state='applying', approved_by=auth.uid(), approved_at=now(), last_error_code=null
      where id=incident_id;

    insert into public.guardian_activity(incident_id, phase, status, summary, detail, visibility, progress, actor)
    values(
      incident_id,
      'applying',
      'waiting',
      'Repair approved. Guardian is preparing an isolated candidate patch.',
      'The repair job may generate and test a candidate, but merge and deployment remain human-controlled.',
      'admin',
      68,
      'approval'
    );
  elsif decision='reject' then
    update public.guardian_incidents
      set state='ignored', approved_by=auth.uid(), approved_at=now()
      where id=incident_id;

    update public.guardian_repairs
      set status='cancelled', completed_at=now(), updated_at=now()
      where incident_id=incident_id and status not in ('merged','cancelled');
  else
    update public.guardian_incidents
      set state='queued', last_error_code=null, attempt_count=attempt_count+1
      where id=incident_id;
  end if;
end;
$$;

revoke all on function public.guardian_decide_incident(uuid,text) from public;
grant execute on function public.guardian_decide_incident(uuid,text) to authenticated;
