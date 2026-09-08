-- Canonical Morley catalogue realtime fan-out.
-- Production was migrated additively first; this file keeps repository schema history aligned.

do $$
begin
  if not exists (select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='device_catalog') then
    alter publication supabase_realtime add table public.device_catalog;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='device_buy_prices') then
    alter publication supabase_realtime add table public.device_buy_prices;
  end if;
  if not exists (select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='inventory_items') then
    alter publication supabase_realtime add table public.inventory_items;
  end if;
end $$;

create table if not exists public.catalog_sync_state (
  id smallint primary key default 1 check (id = 1),
  revision bigint not null default 0,
  changed_at timestamptz not null default now(),
  source_table text,
  operation text
);

insert into public.catalog_sync_state (id, revision, changed_at)
values (1, 0, now())
on conflict (id) do nothing;

alter table public.catalog_sync_state enable row level security;

drop policy if exists authenticated_read_catalog_sync_state on public.catalog_sync_state;
create policy authenticated_read_catalog_sync_state
on public.catalog_sync_state
for select
to authenticated
using (true);

grant select on public.catalog_sync_state to authenticated;

create or replace function public.bump_catalog_sync_revision()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.catalog_sync_state (id, revision, changed_at, source_table, operation)
  values (1, 1, now(), TG_TABLE_NAME, TG_OP)
  on conflict (id) do update
    set revision = public.catalog_sync_state.revision + 1,
        changed_at = excluded.changed_at,
        source_table = excluded.source_table,
        operation = excluded.operation;
  return null;
end;
$$;

revoke all on function public.bump_catalog_sync_revision() from public, anon, authenticated;

drop trigger if exists device_catalog_sync_revision on public.device_catalog;
create trigger device_catalog_sync_revision
after insert or update or delete on public.device_catalog
for each statement execute function public.bump_catalog_sync_revision();

drop trigger if exists device_buy_prices_sync_revision on public.device_buy_prices;
create trigger device_buy_prices_sync_revision
after insert or update or delete on public.device_buy_prices
for each statement execute function public.bump_catalog_sync_revision();

drop trigger if exists inventory_items_sync_revision on public.inventory_items;
create trigger inventory_items_sync_revision
after insert or update or delete on public.inventory_items
for each statement execute function public.bump_catalog_sync_revision();

do $$
begin
  if not exists (select 1 from pg_publication_tables where pubname='supabase_realtime' and schemaname='public' and tablename='catalog_sync_state') then
    alter publication supabase_realtime add table public.catalog_sync_state;
  end if;
end $$;
