begin;

create table if not exists public.restore_points (
  id uuid primary key default gen_random_uuid(),
  source_sha text not null check (source_sha ~ '^[0-9a-fA-F]{40}$'),
  change_ref text not null,
  components jsonb not null default '[]'::jsonb check (jsonb_typeof(components) = 'array'),
  release_refs jsonb not null default '{}'::jsonb check (jsonb_typeof(release_refs) = 'object'),
  web_refs jsonb not null default '{}'::jsonb check (jsonb_typeof(web_refs) = 'object'),
  function_refs jsonb not null default '{}'::jsonb check (jsonb_typeof(function_refs) = 'object'),
  migration_head text,
  config_fingerprints jsonb not null default '{}'::jsonb check (jsonb_typeof(config_fingerprints) = 'object'),
  lifecycle_state text not null default 'captured' check (lifecycle_state in ('captured','deployment_pending','deployed','verified','deployment_failed')),
  created_by uuid not null default auth.uid(),
  created_at timestamptz not null default now()
);

create table if not exists public.restore_events (
  id uuid primary key default gen_random_uuid(),
  restore_point_id uuid not null references public.restore_points(id) on delete restrict,
  event_type text not null check (event_type in ('captured','deployment_pending','deployed','verified','deployment_failed','restore_previewed','restore_approved','restore_executed','restore_failed')),
  components jsonb not null default '[]'::jsonb check (jsonb_typeof(components) = 'array'),
  details jsonb not null default '{}'::jsonb check (jsonb_typeof(details) = 'object'),
  created_by uuid not null default auth.uid(),
  created_at timestamptz not null default now()
);

create index if not exists restore_points_created_at_idx on public.restore_points(created_at desc);
create index if not exists restore_events_restore_point_id_idx on public.restore_events(restore_point_id, created_at desc);

alter table public.restore_points enable row level security;
alter table public.restore_events enable row level security;

revoke all on public.restore_points from anon;
revoke all on public.restore_events from anon;
revoke all on public.restore_points from authenticated;
revoke all on public.restore_events from authenticated;

grant select, insert on public.restore_points to authenticated;
grant select, insert on public.restore_events to authenticated;

drop policy if exists restore_points_admin_read on public.restore_points;
create policy restore_points_admin_read
on public.restore_points
for select
to authenticated
using (private.is_admin_or_manager());

drop policy if exists restore_points_admin_insert on public.restore_points;
create policy restore_points_admin_insert
on public.restore_points
for insert
to authenticated
with check (
  private.is_admin_or_manager()
  and created_by = auth.uid()
);

drop policy if exists restore_events_admin_read on public.restore_events;
create policy restore_events_admin_read
on public.restore_events
for select
to authenticated
using (private.is_admin_or_manager());

drop policy if exists restore_events_admin_insert on public.restore_events;
create policy restore_events_admin_insert
on public.restore_events
for insert
to authenticated
with check (
  private.is_admin_or_manager()
  and created_by = auth.uid()
  and exists (
    select 1
    from public.restore_points rp
    where rp.id = restore_point_id
  )
);

comment on table public.restore_points is 'Immutable Morley deployment restore-point metadata. Production data rollback is intentionally excluded.';
comment on table public.restore_events is 'Append-only audit trail for restore-point lifecycle and protected restore actions.';

commit;
