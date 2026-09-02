-- Dispatch protected repair candidate generation from Postgres rather than relying
-- on an Admin browser session. The one-time nonce mirrors guardian-worker dispatch.

alter table public.guardian_repairs
  add column if not exists dispatch_token uuid not null default gen_random_uuid();

create or replace function private.guardian_rotate_repair_dispatch_token()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private, extensions
as $$
begin
  if new.status = 'requested' then
    new.dispatch_token := gen_random_uuid();
  end if;
  return new;
end;
$$;
revoke all on function private.guardian_rotate_repair_dispatch_token() from public;

drop trigger if exists trg_guardian_rotate_repair_dispatch_token on public.guardian_repairs;
create trigger trg_guardian_rotate_repair_dispatch_token
before update of status on public.guardian_repairs
for each row
when (new.status = 'requested')
execute function private.guardian_rotate_repair_dispatch_token();

create or replace function private.guardian_dispatch_repair_worker()
returns trigger
language plpgsql
security definer
set search_path = pg_catalog, public, private, extensions
as $$
begin
  if new.status <> 'requested' then
    return new;
  end if;

  perform net.http_post(
    url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/guardian-repair-worker',
    headers := jsonb_build_object('Content-Type','application/json'),
    body := jsonb_build_object(
      'incident_id', new.incident_id::text,
      'dispatch_token', new.dispatch_token::text
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

drop trigger if exists trg_guardian_dispatch_repair_worker_insert on public.guardian_repairs;
create trigger trg_guardian_dispatch_repair_worker_insert
after insert on public.guardian_repairs
for each row
when (new.status = 'requested')
execute function private.guardian_dispatch_repair_worker();

drop trigger if exists trg_guardian_dispatch_repair_worker_update on public.guardian_repairs;
create trigger trg_guardian_dispatch_repair_worker_update
after update of status on public.guardian_repairs
for each row
when (new.status = 'requested')
execute function private.guardian_dispatch_repair_worker();

-- Re-dispatch any jobs left stranded by the previous browser-only handoff.
-- The BEFORE trigger rotates the nonce and the AFTER trigger sends the request.
update public.guardian_repairs
set status = 'requested', updated_at = now()
where status = 'requested';
