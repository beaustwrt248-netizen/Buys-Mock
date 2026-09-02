-- Reuse Guardian's established incident dispatch nonce for repair candidate dispatch.
-- This avoids depending on a newly-added repair-table column being visible through
-- the Edge Function Data API schema cache.

create or replace function private.guardian_dispatch_repair_worker()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private, extensions
as $$
declare
  v_dispatch_token uuid;
begin
  if new.status <> 'requested' then
    return new;
  end if;

  select dispatch_token
    into v_dispatch_token
  from public.guardian_incidents
  where id = new.incident_id;

  if v_dispatch_token is null then
    update public.guardian_repairs
    set last_error_code = 'REPAIR_DISPATCH_TOKEN_MISSING', updated_at = now()
    where id = new.id;
    return new;
  end if;

  perform net.http_post(
    url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/guardian-repair-worker',
    headers := jsonb_build_object('Content-Type','application/json'),
    body := jsonb_build_object(
      'incident_id', new.incident_id::text,
      'dispatch_token', v_dispatch_token::text
    ),
    timeout_milliseconds := 5000
  );
  return new;
exception when others then
  update public.guardian_repairs
  set last_error_code = 'REPAIR_LIVE_DISPATCH_FAILED', updated_at = now()
  where id = new.id;
  return new;
end;
$$;
revoke all on function private.guardian_dispatch_repair_worker() from public;

-- Re-dispatch any still-requested repairs using the established incident nonce.
update public.guardian_repairs
set status = 'requested', updated_at = now()
where status = 'requested';
