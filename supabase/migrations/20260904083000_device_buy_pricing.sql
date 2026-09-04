begin;

create table if not exists public.device_buy_prices (
  id uuid primary key default gen_random_uuid(),
  device_catalog_id bigint not null references public.device_catalog(id) on delete cascade,
  storage text not null default '',
  condition_grade text not null default 'base',
  price_aud numeric(10,2),
  authoritative boolean not null default false,
  source text not null default 'Morley pricing',
  notes text not null default '',
  is_active boolean not null default true,
  version integer not null default 1,
  updated_by uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint device_buy_prices_nonnegative check (price_aud is null or price_aud >= 0),
  constraint device_buy_prices_authoritative_requires_price check (not authoritative or price_aud is not null),
  constraint device_buy_prices_unique_variant unique (device_catalog_id, storage, condition_grade)
);

create index if not exists device_buy_prices_device_idx
  on public.device_buy_prices(device_catalog_id, is_active);

create table if not exists public.device_buy_price_history (
  id uuid primary key default gen_random_uuid(),
  device_buy_price_id uuid not null references public.device_buy_prices(id) on delete cascade,
  device_catalog_id bigint not null references public.device_catalog(id) on delete cascade,
  storage text not null default '',
  condition_grade text not null default 'base',
  old_price_aud numeric(10,2),
  new_price_aud numeric(10,2),
  old_authoritative boolean not null default false,
  new_authoritative boolean not null default false,
  source text not null default '',
  notes text not null default '',
  changed_by uuid,
  changed_at timestamptz not null default now()
);

create index if not exists device_buy_price_history_price_idx
  on public.device_buy_price_history(device_buy_price_id, changed_at desc);

alter table public.device_buy_prices enable row level security;
alter table public.device_buy_price_history enable row level security;

revoke all on public.device_buy_prices from anon, authenticated;
revoke all on public.device_buy_price_history from anon, authenticated;

grant all on public.device_buy_prices to service_role;
grant all on public.device_buy_price_history to service_role;

comment on table public.device_buy_prices is 'Protected Morley buy prices linked directly to device_catalog variants. Writes are performed only through admin-pricing-control.';
comment on table public.device_buy_price_history is 'Immutable audit history for protected device buy-price changes.';

commit;
