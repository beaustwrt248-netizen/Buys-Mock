-- Authoritative Morley inventory + realised sales lifecycle.
-- These tables are intentionally separate from Nova's learning ledger.
-- Nova may read verified outcomes, but may not use learned context to mutate these records.

create table if not exists public.inventory_items (
  id uuid primary key default gen_random_uuid(),
  valuation_id uuid references public.valuation_history(id) on delete set null,
  device_catalog_id bigint references public.device_catalog(id) on delete set null,
  item_type text not null check (item_type in ('mobile_phone','tablet','laptop','desktop','console','wearable','other')),
  item_summary text not null check (char_length(trim(item_summary)) between 2 and 240),
  model_number text,
  storage text,
  item_grade text check (item_grade is null or item_grade in ('A','B','C')),
  acquired_price numeric not null check (acquired_price >= 0),
  expected_sale_price numeric check (expected_sale_price is null or expected_sale_price >= 0),
  status text not null default 'in_stock' check (status in ('in_stock','listed','reserved','repair','sold','retired')),
  source text not null default 'manual' check (source in ('manual','valuation','import')),
  acquired_at timestamptz not null default now(),
  listed_at timestamptz,
  retired_at timestamptz,
  notes text check (notes is null or char_length(notes) <= 3000),
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  updated_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists inventory_items_status_idx on public.inventory_items(status, acquired_at desc);
create index if not exists inventory_items_catalog_idx on public.inventory_items(device_catalog_id) where device_catalog_id is not null;
create index if not exists inventory_items_valuation_idx on public.inventory_items(valuation_id) where valuation_id is not null;

create table if not exists public.sales_records (
  id uuid primary key default gen_random_uuid(),
  inventory_item_id uuid not null unique references public.inventory_items(id) on delete restrict,
  valuation_id uuid references public.valuation_history(id) on delete set null,
  acquired_cost numeric not null check (acquired_cost >= 0),
  sold_price numeric not null check (sold_price >= 0),
  fees numeric not null default 0 check (fees >= 0),
  other_costs numeric not null default 0 check (other_costs >= 0),
  realised_profit numeric generated always as (sold_price - acquired_cost - fees - other_costs) stored,
  sales_channel text,
  sold_at timestamptz not null default now(),
  source text not null default 'manual' check (source in ('manual','inventory','import')),
  notes text check (notes is null or char_length(notes) <= 3000),
  created_by uuid references auth.users(id) on delete set null default auth.uid(),
  created_at timestamptz not null default now()
);

create index if not exists sales_records_sold_at_idx on public.sales_records(sold_at desc);
create index if not exists sales_records_valuation_idx on public.sales_records(valuation_id) where valuation_id is not null;

alter table public.inventory_items enable row level security;
alter table public.sales_records enable row level security;

revoke all on public.inventory_items from anon, authenticated;
revoke all on public.sales_records from anon, authenticated;
grant select, insert, update, delete on public.inventory_items to authenticated;
grant select, insert, update, delete on public.sales_records to authenticated;

-- Least privilege: lifecycle records are Admin/Manager only until an explicit staff policy exists.
drop policy if exists inventory_items_admin_manager_select on public.inventory_items;
create policy inventory_items_admin_manager_select on public.inventory_items
for select to authenticated using (private.is_admin_or_manager());

drop policy if exists inventory_items_admin_manager_insert on public.inventory_items;
create policy inventory_items_admin_manager_insert on public.inventory_items
for insert to authenticated with check (private.is_admin_or_manager() and created_by = auth.uid() and updated_by = auth.uid());

drop policy if exists inventory_items_admin_manager_update on public.inventory_items;
create policy inventory_items_admin_manager_update on public.inventory_items
for update to authenticated using (private.is_admin_or_manager())
with check (private.is_admin_or_manager() and updated_by = auth.uid());

drop policy if exists inventory_items_admin_manager_delete on public.inventory_items;
create policy inventory_items_admin_manager_delete on public.inventory_items
for delete to authenticated using (private.is_admin_or_manager());

drop policy if exists sales_records_admin_manager_select on public.sales_records;
create policy sales_records_admin_manager_select on public.sales_records
for select to authenticated using (private.is_admin_or_manager());

drop policy if exists sales_records_admin_manager_insert on public.sales_records;
create policy sales_records_admin_manager_insert on public.sales_records
for insert to authenticated with check (private.is_admin_or_manager() and created_by = auth.uid());

drop policy if exists sales_records_admin_manager_update on public.sales_records;
create policy sales_records_admin_manager_update on public.sales_records
for update to authenticated using (private.is_admin_or_manager())
with check (private.is_admin_or_manager());

drop policy if exists sales_records_admin_manager_delete on public.sales_records;
create policy sales_records_admin_manager_delete on public.sales_records
for delete to authenticated using (private.is_admin_or_manager());

comment on table public.inventory_items is 'Authoritative Morley inventory lifecycle. Nova may read outcomes but learning is non-authoritative and cannot mutate this table.';
comment on table public.sales_records is 'Authoritative realised sales outcomes. Nova may learn from these records but cannot use learning to bypass protected business controls.';
