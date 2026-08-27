-- Privacy-conscious Admin Android health telemetry.
-- Stores only operational crash/error dimensions; no user identifiers, tokens,
-- ticket content, email addresses, stack traces, or free-form error messages.

create table if not exists public.admin_error_events (
  id bigint generated always as identity primary key,
  app_version text not null check (char_length(app_version) between 1 and 40),
  device_model text not null check (char_length(device_model) between 1 and 80),
  failing_screen text not null check (char_length(failing_screen) between 1 and 80),
  error_class text not null check (char_length(error_class) between 1 and 100),
  occurred_at timestamptz not null default now(),
  received_at timestamptz not null default now()
);

create index if not exists admin_error_events_occurred_idx
  on public.admin_error_events(occurred_at desc);

alter table public.admin_error_events enable row level security;

revoke all on public.admin_error_events from anon, authenticated;
grant select, insert on public.admin_error_events to authenticated;

drop policy if exists admin_error_events_admin_select on public.admin_error_events;
create policy admin_error_events_admin_select
on public.admin_error_events
for select
to authenticated
using (private.is_admin_or_manager());

drop policy if exists admin_error_events_admin_insert on public.admin_error_events;
create policy admin_error_events_admin_insert
on public.admin_error_events
for insert
to authenticated
with check (
  private.is_admin_or_manager()
  and occurred_at <= now() + interval '5 minutes'
  and occurred_at >= now() - interval '30 days'
);

comment on table public.admin_error_events is
  'Privacy-minimal Admin Android crash/error telemetry: app version, device model, failing screen, error class, timestamps only.';
