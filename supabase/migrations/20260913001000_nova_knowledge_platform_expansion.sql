-- Nova hybrid knowledge platform expansion.
-- Additive only: preserves nova_knowledge_items and nova_knowledge_revisions as the document registry/history.
-- Production application remains a guarded, human-approved change.

create extension if not exists vector with schema extensions;

create table if not exists public.nova_knowledge_sources (
  id uuid primary key default gen_random_uuid(),
  source_key text not null unique,
  domain text not null default 'general',
  source_type text not null default 'manual',
  source_label text,
  source_uri text,
  trust_level text not null default 'reference' check (trust_level in ('reference','reviewed','verified')),
  authority_score real not null default 0.65 check (authority_score between 0 and 1),
  content_hash text,
  status text not null default 'active' check (status in ('active','archived')),
  observed_at timestamptz,
  retrieved_at timestamptz,
  stale_after timestamptz,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.nova_knowledge_sources is
  'Provenance registry for Nova knowledge. Sensitive identifiers, auth/session material and secrets must never be persisted here.';

create table if not exists public.nova_knowledge_chunks (
  id uuid primary key default gen_random_uuid(),
  knowledge_id uuid not null references public.nova_knowledge_items(id) on delete cascade,
  source_id uuid references public.nova_knowledge_sources(id) on delete set null,
  knowledge_revision integer not null check (knowledge_revision > 0),
  chunk_index integer not null check (chunk_index >= 0),
  content text not null,
  content_hash text not null,
  char_start integer not null default 0 check (char_start >= 0),
  char_end integer not null check (char_end >= char_start),
  token_estimate integer not null default 1 check (token_estimate > 0),
  trust_level text not null default 'reference' check (trust_level in ('reference','reviewed','verified')),
  confidence real not null default 0.70 check (confidence between 0 and 1),
  observed_at timestamptz,
  stale_after timestamptz,
  status text not null default 'active' check (status in ('active','superseded','archived')),
  embedding extensions.vector(384),
  embedding_provider text,
  embedding_model text,
  embedding_dimensions integer,
  embedding_status text not null default 'pending' check (embedding_status in ('pending','ready','error','skipped')),
  embedding_error text,
  embedded_at timestamptz,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  search_vector tsvector generated always as (to_tsvector('english'::regconfig, coalesce(content,''))) stored,
  unique (knowledge_id, knowledge_revision, chunk_index)
);

comment on table public.nova_knowledge_chunks is
  'Deterministic searchable chunks for Nova hybrid retrieval. Embeddings use Supabase gte-small (384 dimensions) initially and keyword search remains a supported fallback.';

create table if not exists public.nova_knowledge_evidence (
  id uuid primary key default gen_random_uuid(),
  chunk_id uuid not null references public.nova_knowledge_chunks(id) on delete cascade,
  source_id uuid references public.nova_knowledge_sources(id) on delete set null,
  evidence_kind text not null default 'source',
  locator text,
  excerpt text,
  confidence real not null default 0.70 check (confidence between 0 and 1),
  metadata jsonb not null default '{}'::jsonb,
  observed_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.nova_knowledge_ingestion_runs (
  id uuid primary key default gen_random_uuid(),
  adapter text not null,
  domain text not null default 'general',
  status text not null default 'running' check (status in ('running','completed','partial','failed','cancelled')),
  checkpoint jsonb not null default '{}'::jsonb,
  scanned_count integer not null default 0 check (scanned_count >= 0),
  created_count integer not null default 0 check (created_count >= 0),
  updated_count integer not null default 0 check (updated_count >= 0),
  skipped_count integer not null default 0 check (skipped_count >= 0),
  error_count integer not null default 0 check (error_count >= 0),
  error_summary text,
  metadata jsonb not null default '{}'::jsonb,
  started_at timestamptz not null default now(),
  completed_at timestamptz,
  created_by uuid references auth.users(id) on delete set null
);

create index if not exists nova_knowledge_sources_domain_status_idx
  on public.nova_knowledge_sources (domain, status, updated_at desc);
create index if not exists nova_knowledge_sources_stale_idx
  on public.nova_knowledge_sources (stale_after) where status = 'active';
create index if not exists nova_knowledge_chunks_knowledge_idx
  on public.nova_knowledge_chunks (knowledge_id, status, knowledge_revision desc, chunk_index);
create index if not exists nova_knowledge_chunks_source_idx
  on public.nova_knowledge_chunks (source_id) where source_id is not null;
create index if not exists nova_knowledge_chunks_search_idx
  on public.nova_knowledge_chunks using gin (search_vector);
create index if not exists nova_knowledge_chunks_embedding_status_idx
  on public.nova_knowledge_chunks (embedding_status, updated_at) where status = 'active';
create index if not exists nova_knowledge_chunks_embedding_hnsw_idx
  on public.nova_knowledge_chunks using hnsw (embedding extensions.vector_cosine_ops)
  where embedding is not null and status = 'active';
create index if not exists nova_knowledge_evidence_chunk_idx
  on public.nova_knowledge_evidence (chunk_id, created_at desc);
create index if not exists nova_knowledge_evidence_source_idx
  on public.nova_knowledge_evidence (source_id) where source_id is not null;
create index if not exists nova_knowledge_ingestion_runs_adapter_idx
  on public.nova_knowledge_ingestion_runs (adapter, started_at desc);

alter table public.nova_knowledge_sources enable row level security;
alter table public.nova_knowledge_chunks enable row level security;
alter table public.nova_knowledge_evidence enable row level security;
alter table public.nova_knowledge_ingestion_runs enable row level security;

revoke all on table public.nova_knowledge_sources from anon, authenticated;
revoke all on table public.nova_knowledge_chunks from anon, authenticated;
revoke all on table public.nova_knowledge_evidence from anon, authenticated;
revoke all on table public.nova_knowledge_ingestion_runs from anon, authenticated;

grant all on table public.nova_knowledge_sources to service_role;
grant all on table public.nova_knowledge_chunks to service_role;
grant all on table public.nova_knowledge_evidence to service_role;
grant all on table public.nova_knowledge_ingestion_runs to service_role;

create or replace function public.nova_search_knowledge_chunks(
  p_query text,
  p_query_embedding extensions.vector(384) default null,
  p_limit integer default 20,
  p_category text default null,
  p_trust_level text default null,
  p_now timestamptz default now()
)
returns table (
  id uuid,
  knowledge_id uuid,
  title text,
  category text,
  content text,
  source_label text,
  trust_level text,
  confidence real,
  observed_at timestamptz,
  stale_after timestamptz,
  lexical_score real,
  semantic_score real
)
language sql
stable
security invoker
set search_path = pg_catalog, extensions
as $$
  with ranked as (
    select
      c.id,
      c.knowledge_id,
      i.title,
      i.category,
      c.content,
      coalesce(s.source_label, i.source_label) as source_label,
      c.trust_level,
      c.confidence,
      c.observed_at,
      c.stale_after,
      case
        when btrim(coalesce(p_query, '')) = '' then 0::real
        else least(1::real, ts_rank_cd(c.search_vector, websearch_to_tsquery('english', p_query))::real)
      end as lexical_score,
      case
        when p_query_embedding is null or c.embedding is null then null::real
        else greatest(0::real, least(1::real, (1 - (c.embedding <=> p_query_embedding))::real))
      end as semantic_score
    from public.nova_knowledge_chunks c
    join public.nova_knowledge_items i on i.id = c.knowledge_id
    left join public.nova_knowledge_sources s on s.id = c.source_id
    where c.status = 'active'
      and i.status = 'active'
      and (p_category is null or p_category = '' or i.category = p_category)
      and (p_trust_level is null or p_trust_level = '' or c.trust_level = p_trust_level)
      and (
        btrim(coalesce(p_query, '')) = ''
        or c.search_vector @@ websearch_to_tsquery('english', p_query)
        or p_query_embedding is not null
      )
  )
  select
    r.id,
    r.knowledge_id,
    r.title,
    r.category,
    r.content,
    r.source_label,
    r.trust_level,
    r.confidence,
    r.observed_at,
    r.stale_after,
    r.lexical_score,
    r.semantic_score
  from ranked r
  order by
    greatest(r.lexical_score, coalesce(r.semantic_score, 0::real)) desc,
    r.lexical_score desc,
    r.knowledge_id,
    r.id
  limit least(greatest(coalesce(p_limit, 20), 1), 100);
$$;

revoke all on function public.nova_search_knowledge_chunks(text, extensions.vector, integer, text, text, timestamptz) from public, anon, authenticated;
grant execute on function public.nova_search_knowledge_chunks(text, extensions.vector, integer, text, text, timestamptz) to service_role;

comment on function public.nova_search_knowledge_chunks(text, extensions.vector, integer, text, text, timestamptz) is
  'Service-role Nova hybrid retrieval primitive. SECURITY INVOKER by design; callers must preserve application authorization and protected-action boundaries.';

create or replace function public.nova_knowledge_health()
returns jsonb
language sql
stable
security invoker
set search_path = pg_catalog
as $$
  select jsonb_build_object(
    'active_sources', (select count(*) from public.nova_knowledge_sources where status = 'active'),
    'stale_sources', (select count(*) from public.nova_knowledge_sources where status = 'active' and stale_after is not null and stale_after < now()),
    'active_chunks', (select count(*) from public.nova_knowledge_chunks where status = 'active'),
    'embedding_ready', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_status = 'ready'),
    'embedding_pending', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_status = 'pending'),
    'embedding_error', (select count(*) from public.nova_knowledge_chunks where status = 'active' and embedding_status = 'error'),
    'embedding_coverage', (
      select case when count(*) = 0 then 0::numeric else round(count(*) filter (where embedding_status = 'ready')::numeric / count(*)::numeric, 4) end
      from public.nova_knowledge_chunks
      where status = 'active'
    ),
    'sources_by_domain', (
      select coalesce(jsonb_object_agg(domain, source_count), '{}'::jsonb)
      from (
        select domain, count(*) as source_count
        from public.nova_knowledge_sources
        where status = 'active'
        group by domain
        order by domain
      ) d
    ),
    'ingestion_runs', (select count(*) from public.nova_knowledge_ingestion_runs),
    'ingestion_failures', (select count(*) from public.nova_knowledge_ingestion_runs where status in ('failed','partial')),
    'latest_ingestion_at', (select max(coalesce(completed_at, started_at)) from public.nova_knowledge_ingestion_runs)
  );
$$;

revoke all on function public.nova_knowledge_health() from public, anon, authenticated;
grant execute on function public.nova_knowledge_health() to service_role;

comment on function public.nova_knowledge_health() is
  'Service-role-only Nova knowledge health summary covering source freshness, embedding coverage and ingestion state without exposing document content.';
