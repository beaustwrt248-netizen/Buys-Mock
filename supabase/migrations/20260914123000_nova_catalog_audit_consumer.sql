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

create or replace function public.nova_complete_catalog_audit(
  p_queue_id bigint,
  p_worker text,
  p_outcome text,
  p_error text default null,
  p_retry_seconds integer default null,
  p_finding jsonb default null
)
returns boolean
language plpgsql
security invoker
set search_path = public
as $$
declare
  v_queue public.nova_catalog_audit_queue%rowtype;
  v_device public.device_catalog%rowtype;
  v_status text;
  v_severity text;
  v_confidence numeric;
  v_source_tier smallint;
begin
  if nullif(btrim(p_worker), '') is null then
    raise exception 'worker identity is required';
  end if;
  if p_outcome not in ('verified','blocked','discrepancy','failed','retry') then
    raise exception 'invalid catalogue audit outcome';
  end if;

  select * into v_queue
  from public.nova_catalog_audit_queue
  where id = p_queue_id
  for update;

  if not found or v_queue.status <> 'in_progress' or v_queue.locked_by <> p_worker then
    return false;
  end if;

  if p_outcome in ('blocked','discrepancy') then
    if p_finding is null then
      raise exception 'finding evidence is required for blocked/discrepancy outcomes';
    end if;
    select * into v_device from public.device_catalog where id = v_queue.device_id;
    if not found then
      raise exception 'catalogue device no longer exists';
    end if;

    v_severity := case when p_finding->>'severity' in ('low','medium','high','critical') then p_finding->>'severity' else 'medium' end;
    v_confidence := greatest(0, least(coalesce(nullif(p_finding->>'confidence','')::numeric, 0), 1));
    v_source_tier := greatest(1, least(coalesce(nullif(p_finding->>'source_tier','')::smallint, 5), 5));

    insert into public.nova_catalog_audit_findings(
      run_id, queue_id, device_id, field_name, current_value, observed_value,
      severity, confidence, source_url, source_name, source_region, source_tier,
      evidence_excerpt, proposed_patch, status, requires_approval
    ) values (
      v_queue.run_id,
      v_queue.id,
      v_queue.device_id,
      coalesce(nullif(p_finding->>'field_name',''), 'identity'),
      p_finding->'current_value',
      p_finding->'observed_value',
      v_severity,
      v_confidence,
      v_device.source_url,
      v_device.source_name,
      v_device.market_region,
      v_source_tier,
      left(coalesce(p_finding->>'evidence_excerpt',''), 1000),
      '{}'::jsonb,
      'open',
      true
    );
  end if;

  if p_outcome = 'retry' then
    update public.nova_catalog_audit_queue
    set status = 'pending',
        next_attempt_at = now() + make_interval(secs => greatest(60, least(coalesce(p_retry_seconds, 300), 86400))),
        locked_at = null,
        locked_by = null,
        last_error = left(p_error, 400),
        updated_at = now()
    where id = p_queue_id and status = 'in_progress' and locked_by = p_worker;
  else
    v_status := p_outcome;
    update public.nova_catalog_audit_queue
    set status = v_status,
        locked_at = null,
        locked_by = null,
        last_error = left(p_error, 400),
        updated_at = now()
    where id = p_queue_id and status = 'in_progress' and locked_by = p_worker;
  end if;

  return found;
end;
$$;

revoke execute on function public.nova_complete_catalog_audit(bigint, text, text, text, integer, jsonb) from public;
revoke execute on function public.nova_complete_catalog_audit(bigint, text, text, text, integer, jsonb) from anon;
revoke execute on function public.nova_complete_catalog_audit(bigint, text, text, text, integer, jsonb) from authenticated;
grant execute on function public.nova_complete_catalog_audit(bigint, text, text, text, integer, jsonb) to service_role;
