-- Mirrors the production Nova knowledge memory migration applied 2026-09-05.
create table if not exists public.nova_knowledge_items (
  id uuid primary key default gen_random_uuid(),
  category text not null default 'general' check (category in ('general','catalogue','pricing','valuation','inventory','sales','support','guardian','release','admin')),
  title text not null,
  content text not null,
  source_type text not null default 'manual' check (source_type in ('manual','file','import')),
  source_label text,
  source_filename text,
  mime_type text,
  version_label text,
  trust_level text not null default 'reference' check (trust_level in ('reference','reviewed','verified')),
  status text not null default 'active' check (status in ('active','archived')),
  content_hash text not null,
  revision integer not null default 1 check (revision > 0),
  metadata jsonb not null default '{}'::jsonb,
  created_by uuid references auth.users(id) on delete set null,
  updated_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  search_vector tsvector generated always as (
    to_tsvector('english'::regconfig,
      coalesce(title,'') || ' ' || coalesce(content,'') || ' ' || coalesce(source_label,'') || ' ' || coalesce(source_filename,'') || ' ' || coalesce(version_label,''))
  ) stored
);

comment on table public.nova_knowledge_items is 'Admin/Manager supplied, non-authoritative Nova AI knowledge. This memory may inform context but cannot override live source-of-truth data, risk classes, approvals, auth, RLS, secrets, or protected Guardian execution.';

create table if not exists public.nova_knowledge_revisions (
  id uuid primary key default gen_random_uuid(),
  knowledge_id uuid not null references public.nova_knowledge_items(id) on delete cascade,
  revision integer not null,
  snapshot jsonb not null,
  changed_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  unique (knowledge_id, revision)
);

create index if not exists nova_knowledge_items_status_category_idx on public.nova_knowledge_items (status, category, updated_at desc);
create index if not exists nova_knowledge_items_hash_idx on public.nova_knowledge_items (content_hash);
create index if not exists nova_knowledge_items_search_idx on public.nova_knowledge_items using gin (search_vector);
create index if not exists nova_knowledge_revisions_item_idx on public.nova_knowledge_revisions (knowledge_id, revision desc);

alter table public.nova_knowledge_items enable row level security;
alter table public.nova_knowledge_revisions enable row level security;
revoke all on table public.nova_knowledge_items from anon, authenticated;
revoke all on table public.nova_knowledge_revisions from anon, authenticated;
grant all on table public.nova_knowledge_items to service_role;
grant all on table public.nova_knowledge_revisions to service_role;
