-- Authenticate Guardian live dispatches with a rotating one-time database nonce.
alter table public.guardian_incidents
  add column if not exists dispatch_token uuid not null default gen_random_uuid();

create or replace function private.guardian_dispatch_live_worker()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private, extensions
as $$
begin
  perform net.http_post(
    url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/guardian-worker',
    headers := jsonb_build_object('Content-Type','application/json'),
    body := jsonb_build_object(
      'incident_id', new.id::text,
      'dispatch_token', new.dispatch_token::text
    ),
    timeout_milliseconds := 5000
  );
  return new;
exception when others then
  update public.guardian_incidents
  set last_error_code = 'LIVE_DISPATCH_FAILED', updated_at = now()
  where id = new.id;
  return new;
end;
$$;
revoke all on function private.guardian_dispatch_live_worker() from public;

drop trigger if exists trg_guardian_dispatch_live_worker on public.guardian_incidents;
create trigger trg_guardian_dispatch_live_worker
after insert on public.guardian_incidents
for each row
when (new.state = 'queued')
execute function private.guardian_dispatch_live_worker();

drop trigger if exists trg_guardian_redispatch_live_worker on public.guardian_incidents;
create trigger trg_guardian_redispatch_live_worker
after update of state on public.guardian_incidents
for each row
when (new.state = 'queued' and old.state is distinct from new.state)
execute function private.guardian_dispatch_live_worker();
