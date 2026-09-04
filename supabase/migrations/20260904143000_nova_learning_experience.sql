create table if not exists public.nova_learning_experiences (
  id uuid primary key default gen_random_uuid(),
  domain text not null check (domain in ('guardian','valuation','catalogue','inventory','sales','support','release','pricing','admin')),
  lesson_key text not null,
  lesson_type text not null,
  summary text not null,
  source_type text not null,
  source_id text,
  evidence jsonb not null default '{}'::jsonb,
  outcome text,
  confidence numeric not null default 0.5 check (confidence >= 0 and confidence <= 1),
  verified boolean not null default false,
  active boolean not null default true,
  observed_at timestamptz,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (domain, lesson_key)
);

comment on table public.nova_learning_experiences is 'Non-authoritative, source-attributed experience ledger for Nova AI. Entries may inform context but never override source-of-truth data, risk classes, approval policy, auth, RLS, secrets, or protected execution boundaries.';

create index if not exists nova_learning_experiences_domain_active_idx
  on public.nova_learning_experiences (domain, active, observed_at desc);
create index if not exists nova_learning_experiences_source_idx
  on public.nova_learning_experiences (source_type, source_id);

alter table public.nova_learning_experiences enable row level security;
revoke all on table public.nova_learning_experiences from anon, authenticated;
grant all on table public.nova_learning_experiences to service_role;
