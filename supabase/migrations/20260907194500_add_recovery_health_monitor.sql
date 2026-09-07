create table if not exists public.recovery_health_findings (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  fingerprint text not null,
  kind text not null check (kind in ('stale_backup','backup_failure','sync_conflict')),
  severity text not null check (severity in ('info','warning','high')),
  status text not null default 'open' check (status in ('open','resolved')),
  summary text not null,
  detail jsonb not null default '{}'::jsonb,
  occurrence_count integer not null default 1 check (occurrence_count > 0),
  first_seen_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  resolved_at timestamptz,
  unique (user_id, fingerprint)
);

create index if not exists recovery_health_findings_user_status_idx
  on public.recovery_health_findings(user_id,status,last_seen_at desc);

alter table public.recovery_health_findings enable row level security;
revoke all on public.recovery_health_findings from anon;
revoke insert,update,delete on public.recovery_health_findings from authenticated;
grant select on public.recovery_health_findings to authenticated;

drop policy if exists "Users read their own recovery findings" on public.recovery_health_findings;
create policy "Users read their own recovery findings"
on public.recovery_health_findings for select to authenticated
using (user_id=(select auth.uid()));

create or replace function public.run_recovery_health_monitor()
returns void
language plpgsql
security definer
set search_path=public,pg_temp
as $$
begin
  insert into public.recovery_health_findings(user_id,fingerprint,kind,severity,status,summary,detail,last_seen_at,resolved_at)
  select b.user_id,'stale-backup','stale_backup','warning','open','Encrypted Google Drive backup is stale',jsonb_build_object('last_backup_at',max(b.created_at),'threshold_hours',36),now(),null
  from public.user_drive_backups b where b.status='ready' group by b.user_id
  having max(b.created_at)<now()-interval '36 hours'
  on conflict(user_id,fingerprint) do update set status='open',severity='warning',summary=excluded.summary,detail=excluded.detail,last_seen_at=now(),resolved_at=null,occurrence_count=public.recovery_health_findings.occurrence_count+1;

  update public.recovery_health_findings f set status='resolved',resolved_at=now(),last_seen_at=now()
  where f.kind='stale_backup' and f.status='open' and exists (
    select 1 from public.user_drive_backups b where b.user_id=f.user_id and b.status='ready' and b.created_at>=now()-interval '36 hours'
  );

  insert into public.recovery_health_findings(user_id,fingerprint,kind,severity,status,summary,detail,last_seen_at,resolved_at)
  select e.user_id,'recent-backup-failure','backup_failure','high','open','Recent encrypted backup failure detected',jsonb_build_object('failures',count(*),'window_hours',6),now(),null
  from public.user_drive_backup_events e where e.event_type='backup_failed' and e.created_at>=now()-interval '6 hours' group by e.user_id
  on conflict(user_id,fingerprint) do update set status='open',severity='high',summary=excluded.summary,detail=excluded.detail,last_seen_at=now(),resolved_at=null,occurrence_count=public.recovery_health_findings.occurrence_count+1;

  update public.recovery_health_findings f set status='resolved',resolved_at=now(),last_seen_at=now()
  where f.kind='backup_failure' and f.status='open' and not exists (
    select 1 from public.user_drive_backup_events e where e.user_id=f.user_id and e.event_type='backup_failed' and e.created_at>=now()-interval '6 hours'
  );

  insert into public.recovery_health_findings(user_id,fingerprint,kind,severity,status,summary,detail,last_seen_at,resolved_at)
  select e.user_id,'repeated-sync-conflict','sync_conflict','warning','open','Repeated cross-device sync conflicts detected',jsonb_build_object('conflicts',count(*),'window_hours',1),now(),null
  from public.user_sync_events e where e.event_type='conflict' and e.created_at>=now()-interval '1 hour' group by e.user_id having count(*)>=3
  on conflict(user_id,fingerprint) do update set status='open',severity='warning',summary=excluded.summary,detail=excluded.detail,last_seen_at=now(),resolved_at=null,occurrence_count=public.recovery_health_findings.occurrence_count+1;

  update public.recovery_health_findings f set status='resolved',resolved_at=now(),last_seen_at=now()
  where f.kind='sync_conflict' and f.status='open' and (
    select count(*) from public.user_sync_events e where e.user_id=f.user_id and e.event_type='conflict' and e.created_at>=now()-interval '1 hour'
  )<3;
end;
$$;

revoke all on function public.run_recovery_health_monitor() from public,anon,authenticated;
grant execute on function public.run_recovery_health_monitor() to service_role;

select cron.unschedule(jobid) from cron.job where jobname='morley-recovery-health-hourly';
select cron.schedule('morley-recovery-health-hourly','17 * * * *','select public.run_recovery_health_monitor();');
select public.run_recovery_health_monitor();
