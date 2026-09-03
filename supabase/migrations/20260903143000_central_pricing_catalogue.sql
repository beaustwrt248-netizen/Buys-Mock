-- Morley central pricing catalogue foundation.
-- Public app clients may read approved catalogue rows. Writes remain protected and are
-- intended to flow through privileged server-side/admin actions using the service role.

create table if not exists public.morley_catalogue_items (
  id uuid primary key default gen_random_uuid(),
  category text not null check (category in ('phone','laptop','console','general')),
  brand text not null default '',
  model text not null,
  model_number text not null default '',
  storage text not null default '',
  price_aud numeric(12,2),
  authoritative boolean not null default false,
  source text not null default 'Morley catalogue',
  is_active boolean not null default true,
  version bigint not null default 1,
  updated_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint morley_catalogue_authorised_price check (not authoritative or price_aud is not null),
  constraint morley_catalogue_nonnegative_price check (price_aud is null or price_aud >= 0)
);

create unique index if not exists morley_catalogue_identity_uq
  on public.morley_catalogue_items(category, lower(brand), lower(model), lower(model_number), lower(storage));
create index if not exists morley_catalogue_search_idx
  on public.morley_catalogue_items(category, brand, model, model_number, storage) where is_active;

create table if not exists public.morley_price_history (
  id bigint generated always as identity primary key,
  catalogue_item_id uuid not null references public.morley_catalogue_items(id) on delete cascade,
  old_price_aud numeric(12,2),
  new_price_aud numeric(12,2),
  old_authoritative boolean not null,
  new_authoritative boolean not null,
  changed_by uuid references auth.users(id) on delete set null,
  changed_at timestamptz not null default now(),
  reason text not null default ''
);
create index if not exists morley_price_history_item_idx
  on public.morley_price_history(catalogue_item_id, changed_at desc);

create or replace function public.morley_capture_price_history()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if old.price_aud is distinct from new.price_aud
     or old.authoritative is distinct from new.authoritative then
    insert into public.morley_price_history(
      catalogue_item_id, old_price_aud, new_price_aud,
      old_authoritative, new_authoritative, changed_by, reason
    ) values (
      new.id, old.price_aud, new.price_aud,
      old.authoritative, new.authoritative, new.updated_by, ''
    );
  end if;
  new.version := old.version + 1;
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists morley_catalogue_price_audit on public.morley_catalogue_items;
create trigger morley_catalogue_price_audit
before update on public.morley_catalogue_items
for each row execute function public.morley_capture_price_history();

alter table public.morley_catalogue_items enable row level security;
alter table public.morley_price_history enable row level security;

drop policy if exists "authenticated can read active morley catalogue" on public.morley_catalogue_items;
create policy "authenticated can read active morley catalogue"
on public.morley_catalogue_items for select
to authenticated
using (is_active = true);

drop policy if exists "authenticated can read morley price history" on public.morley_price_history;
create policy "authenticated can read morley price history"
on public.morley_price_history for select
to authenticated
using (true);

-- Deliberately no INSERT/UPDATE/DELETE policy for authenticated clients.
-- Admin writes must remain server-side/service-role controlled so a normal APK/web
-- session cannot promote an unpriced catalogue item into an authorised buy price.
