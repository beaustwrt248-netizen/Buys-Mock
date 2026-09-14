-- Atomic, bounded claim primitive for the Nova catalogue-audit worker.
-- Enqueue scheduling remains intentionally disabled until the consumer is deployed and verified.

create or replace function public.nova_claim_catalog_audits(
  p_worker text,
  p_limit integer default 10,
  p_stale_seconds integer default 900
)
returns table (
  id bigint,
  run_id uuid,
  device_id bigint,
  reason text,
  attempt_count integer,
  source_url text,
  source_name text,
  brand text,
  model_name text,
  model_number text,
  release_year integer,
  key_specs jsonb,
  market_region text
)
language plpgsql
security invoker
set search_path = public
as $$
begin
  if nullif(btrim(p_worker), '') is null then
    raise exception 'worker identity is required';
  end if;

  return query
  with candidates as (
    select q.id
    from public.nova_catalog_audit_queue q
    where (
      q.status = 'pending'
      and q.next_attempt_at <= now()
    ) or (
      q.status = 'in_progress'
      and q.locked_at is not null
      and q.locked_at <= now() - make_interval(secs => greatest(60, least(coalesce(p_stale_seconds, 900), 86400)))
    )
    order by q.priority desc, q.next_attempt_at asc, q.created_at asc
    for update skip locked
    limit greatest(1, least(coalesce(p_limit, 10), 25))
  ), claimed as (
    update public.nova_catalog_audit_queue q
    set status = 'in_progress',
        attempt_count = q.attempt_count + 1,
        locked_at = now(),
        locked_by = left(btrim(p_worker), 120),
        last_error = null,
        updated_at = now()
    from candidates c
    where q.id = c.id
    returning q.id, q.run_id, q.device_id, q.reason, q.attempt_count
  )
  select c.id,
         c.run_id,
         c.device_id,
         c.reason,
         c.attempt_count,
         d.source_url,
         d.source_name,
         d.brand,
         d.model_name,
         d.model_number,
         d.release_year,
         d.key_specs,
         d.market_region
  from claimed c
  join public.device_catalog d on d.id = c.device_id
  order by c.id;
end;
$$;

revoke execute on function public.nova_claim_catalog_audits(text, integer, integer) from public;
revoke execute on function public.nova_claim_catalog_audits(text, integer, integer) from anon;
revoke execute on function public.nova_claim_catalog_audits(text, integer, integer) from authenticated;
grant execute on function public.nova_claim_catalog_audits(text, integer, integer) to service_role;
