-- Nova knowledge maintenance hardening.
-- Additive only: makes embedding claims concurrency-safe and retry-aware.

alter table public.nova_knowledge_chunks
  add column if not exists embedding_attempt_count integer not null default 0,
  add column if not exists embedding_last_attempt_at timestamptz,
  add column if not exists embedding_next_retry_at timestamptz,
  add column if not exists embedding_last_error_code text;

create index if not exists nova_knowledge_chunks_embedding_retry_idx
  on public.nova_knowledge_chunks (embedding_status, embedding_next_retry_at, updated_at)
  where status = 'active' and embedding_status <> 'ready';

create or replace function public.nova_claim_embedding_chunks(p_limit integer default 20)
returns table (
  id uuid,
  content text,
  embedding_attempt_count integer
)
language plpgsql
volatile
security invoker
set search_path = pg_catalog
as $$
declare
  v_limit integer := least(greatest(coalesce(p_limit, 20), 1), 20);
begin
  return query
  with candidates as (
    select c.id
    from public.nova_knowledge_chunks c
    where c.status = 'active'
      and c.embedding_status <> 'ready'
      and (
        c.embedding_status = 'pending'
        or (
          c.embedding_status = 'error'
          and c.embedding_attempt_count < 8
          and coalesce(c.embedding_next_retry_at, '-infinity'::timestamptz) <= now()
        )
      )
    order by
      case when c.embedding_status = 'pending' then 0 else 1 end,
      coalesce(c.embedding_next_retry_at, c.updated_at),
      c.id
    limit v_limit
    for update skip locked
  ), claimed as (
    update public.nova_knowledge_chunks c
    set embedding_attempt_count = c.embedding_attempt_count + 1,
        embedding_last_attempt_at = now(),
        embedding_next_retry_at = now() + interval '15 minutes',
        updated_at = now()
    from candidates q
    where c.id = q.id
    returning c.id, c.content, c.embedding_attempt_count
  )
  select claimed.id, claimed.content, claimed.embedding_attempt_count
  from claimed;
end;
$$;

revoke all on function public.nova_claim_embedding_chunks(integer) from public, anon, authenticated;
grant execute on function public.nova_claim_embedding_chunks(integer) to service_role;

comment on function public.nova_claim_embedding_chunks(integer) is
  'Service-role-only bounded embedding claim using SKIP LOCKED. It never changes Nova knowledge content.';

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
    'embedding_retryable', (
      select count(*) from public.nova_knowledge_chunks
      where status = 'active'
        and embedding_status = 'error'
        and embedding_attempt_count < 8
        and coalesce(embedding_next_retry_at, '-infinity'::timestamptz) <= now()
    ),
    'embedding_attempted', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_attempt_count > 0),
    'latest_embedding_attempt_at', (select max(embedding_last_attempt_at) from public.nova_knowledge_chunks where status = 'active'),
    'embedding_coverage', (
      select case when count(*) = 0 then 0::numeric
        else round(count(*) filter (where embedding_status = 'ready')::numeric / count(*)::numeric, 4)
      end
      from public.nova_knowledge_chunks where status = 'active'
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
      select status from public.nova_knowledge_maintenance_runs order by started_at desc limit 1
    ),
    'maintenance_failures_24h', (
      select count(*) from public.nova_knowledge_maintenance_runs
      where started_at >= now() - interval '24 hours' and status in ('failed','partial')
    )
  );
$function$;

revoke all on function public.nova_knowledge_health() from public, anon, authenticated;
grant execute on function public.nova_knowledge_health() to service_role;
