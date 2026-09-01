begin;

create table if not exists public.invite_redemption_rate_limits (
  key_hash text primary key check (key_hash ~ '^[0-9a-f]{64}$'),
  window_started_at timestamptz not null default now(),
  attempts integer not null default 0 check (attempts >= 0),
  updated_at timestamptz not null default now()
);

alter table public.invite_redemption_rate_limits enable row level security;
revoke all on table public.invite_redemption_rate_limits from public, anon, authenticated;

create or replace function public.consume_invite_redemption_rate_limit(
  rate_key_hash text,
  maximum_attempts integer default 5,
  window_seconds integer default 900
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $function$
declare
  accepted boolean;
begin
  if rate_key_hash !~ '^[0-9a-f]{64}$'
     or maximum_attempts < 1 or maximum_attempts > 20
     or window_seconds < 60 or window_seconds > 86400 then
    return false;
  end if;

  insert into public.invite_redemption_rate_limits as limits
    (key_hash, window_started_at, attempts, updated_at)
  values (rate_key_hash, now(), 1, now())
  on conflict (key_hash) do update
  set window_started_at = case
        when limits.window_started_at <= now() - make_interval(secs => window_seconds)
        then now() else limits.window_started_at end,
      attempts = case
        when limits.window_started_at <= now() - make_interval(secs => window_seconds)
        then 1 else limits.attempts + 1 end,
      updated_at = now()
  returning attempts <= maximum_attempts into accepted;

  delete from public.invite_redemption_rate_limits
  where updated_at < now() - interval '7 days';

  return accepted;
end;
$function$;

revoke all on function public.consume_invite_redemption_rate_limit(text,integer,integer) from public, anon, authenticated;
grant execute on function public.consume_invite_redemption_rate_limit(text,integer,integer) to service_role;

alter function public.search_laptop_models(text, integer) security invoker;

create index if not exists invite_redemption_rate_limits_updated_idx
  on public.invite_redemption_rate_limits(updated_at);


create extension if not exists pg_cron with schema pg_catalog;

create or replace function public.purge_expired_privacy_data()
returns jsonb
language plpgsql
security definer
set search_path = ''
as $function$
declare
  rate_limit_rows integer;
  team_invite_rows integer;
  download_invite_rows integer;
  telemetry_rows integer;
begin
  delete from public.invite_redemption_rate_limits where updated_at < now() - interval '7 days';
  get diagnostics rate_limit_rows = row_count;

  delete from public.app_invites
  where (used_at is not null and used_at < now() - interval '30 days')
     or (used_at is null and expires_at < now() - interval '30 days');
  get diagnostics team_invite_rows = row_count;

  delete from public.app_download_invites
  where (redeemed_at is not null and redeemed_at < now() - interval '30 days')
     or (revoked_at is not null and revoked_at < now() - interval '30 days')
     or (redeemed_at is null and revoked_at is null and expires_at < now() - interval '30 days');
  get diagnostics download_invite_rows = row_count;

  delete from public.admin_error_events where received_at < now() - interval '90 days';
  get diagnostics telemetry_rows = row_count;

  return jsonb_build_object(
    'rate_limits', rate_limit_rows,
    'team_invites', team_invite_rows,
    'download_invites', download_invite_rows,
    'telemetry', telemetry_rows
  );
end;
$function$;

revoke all on function public.purge_expired_privacy_data() from public, anon, authenticated;
grant execute on function public.purge_expired_privacy_data() to service_role;

select cron.unschedule(jobid)
from cron.job
where jobname = 'buys-privacy-retention-daily';

select cron.schedule(
  'buys-privacy-retention-daily',
  '17 3 * * *',
  $cron$select public.purge_expired_privacy_data();$cron$
);

commit;
