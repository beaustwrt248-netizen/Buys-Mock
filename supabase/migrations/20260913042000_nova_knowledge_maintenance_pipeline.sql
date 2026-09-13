-- Nova knowledge maintenance pipeline
-- Additive run-state + five-minute Vault-backed scheduler.

create table if not exists public.nova_knowledge_maintenance_runs (
  id uuid primary key default gen_random_uuid(),
  status text not null default 'running' check (status in ('running','completed','partial','failed')),
  embedded_ready integer not null default 0 check (embedded_ready >= 0),
  embedded_error integer not null default 0 check (embedded_error >= 0),
  ingested_created integer not null default 0 check (ingested_created >= 0),
  ingested_updated integer not null default 0 check (ingested_updated >= 0),
  ingested_skipped integer not null default 0 check (ingested_skipped >= 0),
  ingested_error integer not null default 0 check (ingested_error >= 0),
  error_summary text,
  started_at timestamptz not null default now(),
  completed_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists idx_nova_knowledge_maintenance_runs_started_at
  on public.nova_knowledge_maintenance_runs (started_at desc);

alter table public.nova_knowledge_maintenance_runs enable row level security;
revoke all on table public.nova_knowledge_maintenance_runs from public, anon, authenticated;
grant select, insert, update on table public.nova_knowledge_maintenance_runs to service_role;

create or replace function public.nova_knowledge_health()
returns jsonb
language sql
stable
security invoker
set search_path to 'pg_catalog'
as $function$
  select jsonb_build_object(
    'active_sources', (select count(*) from public.nova_knowledge_sources where status = 'active'),
    'stale_sources', (select count(*) from public.nova_knowledge_sources where status = 'active' and stale_after is not null and stale_after < now()),
    'active_chunks', (select count(*) from public.nova_knowledge_chunks where status = 'active'),
    'embedding_ready', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_status = 'ready'),
    'embedding_pending', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_status = 'pending'),
    'embedding_error', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_status = 'error'),
    'embedding_coverage', (
      select case
        when count(*) = 0 then 0::numeric
        else round(count(*) filter (where embedding_status = 'ready')::numeric / count(*)::numeric, 4)
      end
      from public.nova_knowledge_chunks
      where status = 'active'
    ),
    'sources_by_domain', (
      select coalesce(jsonb_object_agg(domain, source_count), '{}'::jsonb)
      from (
        select domain, count(*) source_count
        from public.nova_knowledge_sources
        where status = 'active'
        group by domain
        order by domain
      ) d
    ),
    'ingestion_runs', (select count(*) from public.nova_knowledge_ingestion_runs),
    'ingestion_failures', (select count(*) from public.nova_knowledge_ingestion_runs where status in ('failed','partial')),
    'latest_ingestion_at', (select max(coalesce(completed_at, started_at)) from public.nova_knowledge_ingestion_runs),
    'latest_maintenance_at', (select max(coalesce(completed_at, started_at)) from public.nova_knowledge_maintenance_runs),
    'latest_maintenance_status', (
      select status
      from public.nova_knowledge_maintenance_runs
      order by started_at desc
      limit 1
    ),
    'maintenance_failures_24h', (
      select count(*)
      from public.nova_knowledge_maintenance_runs
      where started_at >= now() - interval '24 hours'
        and status in ('failed','partial')
    )
  );
$function$;

-- Keep the schedule idempotent across replays/redeployments.
do $block$
declare
  existing_job record;
begin
  for existing_job in
    select jobid from cron.job where jobname = 'nova-knowledge-maintenance-every-5-minutes'
  loop
    perform cron.unschedule(existing_job.jobid);
  end loop;
end
$block$;

-- The scheduler credential already exists in production Vault and is shared with
-- the established server-to-server backup scheduler. No credential value is stored
-- in this migration. Environments without the Vault secret simply do not schedule
-- the job; the function can still be deployed/tested independently.
select cron.schedule(
  'nova-knowledge-maintenance-every-5-minutes',
  '*/5 * * * *',
  $cron$
    select net.http_post(
      url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/nova-knowledge-maintenance',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'x-maintenance-secret', (
          select decrypted_secret
          from vault.decrypted_secrets
          where name = 'morley_backup_scheduler_secret'
          limit 1
        )
      ),
      body := '{"action":"run","embedding_limit":20,"ingest_limit":12}'::jsonb,
      timeout_milliseconds := 50000
    ) as request_id;
  $cron$
)
where exists (
  select 1
  from vault.secrets
  where name = 'morley_backup_scheduler_secret'
);
