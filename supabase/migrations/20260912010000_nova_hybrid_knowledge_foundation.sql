-- Nova hybrid knowledge foundation.
-- High-risk production migration: prepare/review in PR; do not apply without explicit approval.
create extension if not exists vector with schema extensions;

alter table public.nova_knowledge_items
  add column if not exists source_uri text,
  add column if not exists external_key text,
  add column if not exists source_updated_at timestamptz,
  add column if not exists fresh_until timestamptz,
  add column if not exists confidence numeric not null default 0.5 check (confidence >= 0 and confidence <= 1),
  add column if not exists authority_weight numeric not null default 0.5 check (authority_weight >= 0 and authority_weight <= 1);

alter table public.nova_knowledge_items
  drop constraint if exists nova_knowledge_items_category_check;
alter table public.nova_knowledge_items
  add constraint nova_knowledge_items_category_check check (
    category in (
      'general','catalogue','pricing','valuation','inventory','sales','support','guardian','release','admin',
      'development','operations','research','policy','product'
    )
  );

alter table public.nova_knowledge_items
  drop constraint if exists nova_knowledge_items_source_type_check;
alter table public.nova_knowledge_items
  add constraint nova_knowledge_items_source_type_check check (
    source_type in ('manual','file','import','system','web','integration')
  );

create unique index if not exists nova_knowledge_items_external_key_idx
  on public.nova_knowledge_items (category, source_type, external_key)
  where external_key is not null;
create index if not exists nova_knowledge_items_freshness_idx
  on public.nova_knowledge_items (status, fresh_until, updated_at desc);

create table if not exists public.nova_knowledge_chunks (
  id uuid primary key default gen_random_uuid(),
  knowledge_id uuid not null references public.nova_knowledge_items(id) on delete cascade,
  chunk_index integer not null check (chunk_index >= 0),
  content text not null,
  content_hash text not null,
  token_estimate integer not null default 0 check (token_estimate >= 0),
  embedding extensions.vector(1536),
  embedding_model text,
  embedded_at timestamptz,
  active boolean not null default true,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  search_vector tsvector generated always as (to_tsvector('english'::regconfig, coalesce(content, ''))) stored,
  unique (knowledge_id, chunk_index)
);
comment on table public.nova_knowledge_chunks is 'Derived Nova retrieval chunks. Service-role only; parent knowledge remains advisory and cannot override protected Morley source-of-truth or approval boundaries.';

create index if not exists nova_knowledge_chunks_knowledge_idx
  on public.nova_knowledge_chunks (knowledge_id, active, chunk_index);
create index if not exists nova_knowledge_chunks_hash_idx
  on public.nova_knowledge_chunks (content_hash);
create index if not exists nova_knowledge_chunks_search_idx
  on public.nova_knowledge_chunks using gin (search_vector);
create index if not exists nova_knowledge_chunks_embedding_idx
  on public.nova_knowledge_chunks using hnsw (embedding extensions.vector_cosine_ops);

alter table public.nova_knowledge_chunks enable row level security;
revoke all on table public.nova_knowledge_chunks from anon, authenticated;
grant all on table public.nova_knowledge_chunks to service_role;

-- Make all existing active Nova knowledge immediately available to the new retrieval layer.
-- Full semantic embedding is filled asynchronously/on-touch by the Edge Function.
insert into public.nova_knowledge_chunks (
  knowledge_id,
  chunk_index,
  content,
  content_hash,
  token_estimate,
  active,
  metadata
)
select
  id,
  0,
  content,
  content_hash,
  greatest(1, ceil(length(content)::numeric / 4.0)::integer),
  true,
  jsonb_build_object('backfilled_from_parent', true)
from public.nova_knowledge_items
where status = 'active'
on conflict (knowledge_id, chunk_index) do nothing;

create or replace function public.nova_match_knowledge_hybrid(
  query_text text,
  query_embedding extensions.vector(1536) default null,
  match_count integer default 20,
  category_filter text default null,
  full_text_weight double precision default 1.0,
  semantic_weight double precision default 1.0,
  rrf_k integer default 50
)
returns table (
  knowledge_id uuid,
  chunk_id uuid,
  category text,
  title text,
  content text,
  source_type text,
  source_label text,
  source_uri text,
  trust_level text,
  confidence numeric,
  authority_weight numeric,
  fresh_until timestamptz,
  lexical_rank bigint,
  semantic_rank bigint,
  semantic_similarity double precision,
  score double precision
)
language sql
stable
security invoker
set search_path = public, extensions
as $$
with lexical as (
  select
    c.id as chunk_id,
    row_number() over (
      order by ts_rank_cd(c.search_vector, websearch_to_tsquery('english', query_text)) desc, c.id
    ) as rank_ix
  from public.nova_knowledge_chunks c
  join public.nova_knowledge_items i on i.id = c.knowledge_id
  where c.active
    and i.status = 'active'
    and (category_filter is null or i.category = category_filter)
    and nullif(btrim(query_text), '') is not null
    and c.search_vector @@ websearch_to_tsquery('english', query_text)
  order by rank_ix
  limit greatest(20, least(500, match_count * 8))
),
semantic as (
  select
    c.id as chunk_id,
    row_number() over (order by c.embedding <=> query_embedding, c.id) as rank_ix,
    (1.0 - (c.embedding <=> query_embedding))::double precision as similarity
  from public.nova_knowledge_chunks c
  join public.nova_knowledge_items i on i.id = c.knowledge_id
  where query_embedding is not null
    and c.embedding is not null
    and c.active
    and i.status = 'active'
    and (category_filter is null or i.category = category_filter)
  order by c.embedding <=> query_embedding, c.id
  limit greatest(20, least(500, match_count * 8))
),
candidate_ids as (
  select chunk_id from lexical
  union
  select chunk_id from semantic
),
ranked as (
  select
    i.id as knowledge_id,
    c.id as chunk_id,
    i.category,
    i.title,
    c.content,
    i.source_type,
    i.source_label,
    i.source_uri,
    i.trust_level,
    i.confidence,
    i.authority_weight,
    i.fresh_until,
    l.rank_ix as lexical_rank,
    s.rank_ix as semantic_rank,
    s.similarity as semantic_similarity,
    (
      coalesce(full_text_weight / (greatest(1, rrf_k) + l.rank_ix), 0.0) +
      coalesce(semantic_weight / (greatest(1, rrf_k) + s.rank_ix), 0.0)
    )
    * (0.70 + 0.30 * i.confidence::double precision)
    * (0.75 + 0.25 * i.authority_weight::double precision)
    * case
        when i.fresh_until is null then 1.0
        when i.fresh_until >= now() then 1.05
        else 0.80
      end as fused_score
  from candidate_ids x
  join public.nova_knowledge_chunks c on c.id = x.chunk_id
  join public.nova_knowledge_items i on i.id = c.knowledge_id
  left join lexical l on l.chunk_id = c.id
  left join semantic s on s.chunk_id = c.id
)
select
  ranked.knowledge_id,
  ranked.chunk_id,
  ranked.category,
  ranked.title,
  ranked.content,
  ranked.source_type,
  ranked.source_label,
  ranked.source_uri,
  ranked.trust_level,
  ranked.confidence,
  ranked.authority_weight,
  ranked.fresh_until,
  ranked.lexical_rank,
  ranked.semantic_rank,
  ranked.semantic_similarity,
  ranked.fused_score as score
from ranked
order by ranked.fused_score desc, ranked.knowledge_id, ranked.chunk_id
limit greatest(1, least(100, match_count));
$$;

revoke all on function public.nova_match_knowledge_hybrid(
  text, extensions.vector, integer, text, double precision, double precision, integer
) from public, anon, authenticated;
grant execute on function public.nova_match_knowledge_hybrid(
  text, extensions.vector, integer, text, double precision, double precision, integer
) to service_role;
