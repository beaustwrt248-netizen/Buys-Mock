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

commit;
