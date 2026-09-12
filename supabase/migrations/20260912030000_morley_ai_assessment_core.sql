begin;

create table if not exists public.device_assessments (
  id uuid primary key default gen_random_uuid(),
  created_by uuid not null default auth.uid() references auth.users(id),
  catalogue_ref text,
  stock_ref text,
  state text not null default 'draft' check (state in (
    'draft','evidence_ready','identified','diagnostics_ready','proposed','review_required',
    'approved','stock_prepared','completed','identity_unresolved','storage_unresolved',
    'evidence_insufficient','policy_blocked'
  )),
  resolved_model text,
  resolved_storage_gb integer check (resolved_storage_gb is null or resolved_storage_gb >= 0),
  identity_confidence numeric check (identity_confidence is null or identity_confidence between 0 and 1),
  cosmetic_score numeric check (cosmetic_score is null or cosmetic_score between 0 and 100),
  functional_score numeric check (functional_score is null or functional_score between 0 and 100),
  condition_score numeric check (condition_score is null or condition_score between 0 and 100),
  condition_rules_version text,
  recommended_grade text,
  final_grade text,
  valuation_confidence numeric check (valuation_confidence is null or valuation_confidence between 0 and 1),
  proposed_buy_price_cents bigint check (proposed_buy_price_cents is null or proposed_buy_price_cents >= 0),
  final_buy_price_cents bigint check (final_buy_price_cents is null or final_buy_price_cents >= 0),
  repair_decision text check (repair_decision is null or repair_decision in ('buy_repair','buy_as_is','parts_only','review')),
  grade_confirmed_by uuid references auth.users(id),
  grade_confirmed_at timestamptz,
  buy_price_confirmed_by uuid references auth.users(id),
  buy_price_confirmed_at timestamptz,
  repair_decision_confirmed_by uuid references auth.users(id),
  repair_decision_confirmed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.assessment_evidence (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.device_assessments(id) on delete cascade,
  evidence_type text not null,
  source text not null,
  storage_path text,
  confidence numeric not null default 0 check (confidence between 0 and 1),
  verified boolean not null default false,
  verification_status text not null default 'pending' check (verification_status in ('pending','verified','rejected','superseded')),
  capture_quality jsonb not null default '{}'::jsonb,
  metadata jsonb not null default '{}'::jsonb,
  protected_identifier_ref text,
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);

create table if not exists public.damage_findings (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.device_assessments(id) on delete cascade,
  evidence_id uuid references public.assessment_evidence(id) on delete cascade,
  damage_type text not null,
  severity text not null check (severity in ('low','medium','high','critical')),
  confidence numeric not null default 0 check (confidence between 0 and 1),
  region jsonb not null default '{}'::jsonb,
  review_status text not null default 'pending' check (review_status in ('pending','accepted','rejected','amended')),
  reviewed_by uuid references auth.users(id),
  reviewed_at timestamptz,
  original_finding jsonb not null default '{}'::jsonb,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.diagnostic_results (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.device_assessments(id) on delete cascade,
  test_type text not null,
  status text not null check (status in ('pass','fail','unknown','not_tested')),
  severity text not null default 'low' check (severity in ('low','medium','high','critical')),
  measurement jsonb not null default '{}'::jsonb,
  confidence numeric not null default 0 check (confidence between 0 and 1),
  source text not null default 'guided',
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);

create table if not exists public.valuation_quotes (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.device_assessments(id) on delete cascade,
  base_market_value_cents bigint check (base_market_value_cents is null or base_market_value_cents >= 0),
  condition_adjustment_cents bigint,
  repair_estimate_cents bigint check (repair_estimate_cents is null or repair_estimate_cents >= 0),
  stock_adjustment_cents bigint,
  demand_adjustment_cents bigint,
  target_resale_cents bigint check (target_resale_cents is null or target_resale_cents >= 0),
  proposed_buy_cents bigint check (proposed_buy_cents is null or proposed_buy_cents >= 0),
  expected_margin_cents bigint,
  confidence numeric not null default 0 check (confidence between 0 and 1),
  recommendation text not null check (recommendation in ('buy_repair','buy_as_is','parts_only','review')),
  explanation jsonb not null default '{}'::jsonb,
  rules_version text not null,
  model_version text,
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);

create table if not exists public.deal_risk_flags (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.device_assessments(id) on delete cascade,
  risk_type text not null,
  severity text not null check (severity in ('low','medium','high','critical')),
  explanation text not null,
  status text not null default 'open' check (status in ('open','resolved','dismissed')),
  resolved_by uuid references auth.users(id),
  resolved_at timestamptz,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.staff_overrides (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid not null references public.device_assessments(id) on delete cascade,
  field_name text not null,
  previous_value jsonb,
  new_value jsonb not null,
  reason text not null,
  actor_user_id uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);

create table if not exists public.device_passports (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid unique references public.device_assessments(id) on delete set null,
  stock_ref text,
  status text not null default 'active' check (status in ('active','sold','archived')),
  created_by uuid not null default auth.uid() references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.device_passport_events (
  id uuid primary key default gen_random_uuid(),
  passport_id uuid not null references public.device_passports(id) on delete cascade,
  assessment_id uuid references public.device_assessments(id) on delete set null,
  event_type text not null,
  details jsonb not null default '{}'::jsonb,
  actor_user_id uuid default auth.uid() references auth.users(id),
  source text not null default 'staff',
  version text,
  created_at timestamptz not null default now()
);

create table if not exists public.ai_model_versions (
  id uuid primary key default gen_random_uuid(),
  capability text not null,
  provider text not null,
  model_name text not null,
  model_version text not null,
  rules_version text,
  config_hash text,
  active boolean not null default true,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  unique (capability, provider, model_name, model_version)
);

create table if not exists public.ai_decision_audit (
  id uuid primary key default gen_random_uuid(),
  assessment_id uuid references public.device_assessments(id) on delete set null,
  decision_type text not null,
  capability text not null,
  model_version text,
  rules_version text,
  input_refs jsonb not null default '[]'::jsonb,
  output jsonb not null default '{}'::jsonb,
  confidence numeric check (confidence is null or confidence between 0 and 1),
  approval_status text not null default 'pending' check (approval_status in ('pending','approved','rejected','superseded')),
  actor_user_id uuid default auth.uid() references auth.users(id),
  created_at timestamptz not null default now()
);

create index if not exists device_assessments_state_created_idx on public.device_assessments (state, created_at desc);
create index if not exists assessment_evidence_assessment_idx on public.assessment_evidence (assessment_id, created_at);
create index if not exists damage_findings_assessment_idx on public.damage_findings (assessment_id, created_at);
create index if not exists diagnostic_results_assessment_idx on public.diagnostic_results (assessment_id, created_at);
create index if not exists valuation_quotes_assessment_idx on public.valuation_quotes (assessment_id, created_at desc);
create index if not exists deal_risk_flags_assessment_status_idx on public.deal_risk_flags (assessment_id, status);
create index if not exists staff_overrides_assessment_idx on public.staff_overrides (assessment_id, created_at);
create index if not exists device_passports_stock_ref_idx on public.device_passports (stock_ref);
create index if not exists device_passport_events_passport_idx on public.device_passport_events (passport_id, created_at);
create index if not exists ai_decision_audit_assessment_idx on public.ai_decision_audit (assessment_id, created_at);

alter table public.device_assessments enable row level security;
alter table public.assessment_evidence enable row level security;
alter table public.damage_findings enable row level security;
alter table public.diagnostic_results enable row level security;
alter table public.valuation_quotes enable row level security;
alter table public.deal_risk_flags enable row level security;
alter table public.staff_overrides enable row level security;
alter table public.device_passports enable row level security;
alter table public.device_passport_events enable row level security;
alter table public.ai_model_versions enable row level security;
alter table public.ai_decision_audit enable row level security;

-- Morley staff access is intentionally limited to the repository's existing role helpers.
create policy "Morley staff read device assessments" on public.device_assessments
  for select to authenticated
  using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create device assessments" on public.device_assessments
  for insert to authenticated
  with check ((private.is_admin_or_manager() or private.is_support_staff()) and created_by = auth.uid());
create policy "Morley staff update device assessments" on public.device_assessments
  for update to authenticated
  using (private.is_admin_or_manager() or private.is_support_staff())
  with check (private.is_admin_or_manager() or private.is_support_staff());

grant select, insert, update on table public.device_assessments to authenticated;

create policy "Morley staff read assessment evidence" on public.assessment_evidence
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create assessment evidence" on public.assessment_evidence
  for insert to authenticated with check ((private.is_admin_or_manager() or private.is_support_staff()) and created_by = auth.uid());
create policy "Morley staff update assessment evidence" on public.assessment_evidence
  for update to authenticated using (private.is_admin_or_manager() or private.is_support_staff())
  with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert, update on table public.assessment_evidence to authenticated;

create policy "Morley staff read damage findings" on public.damage_findings
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create damage findings" on public.damage_findings
  for insert to authenticated with check (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff update damage findings" on public.damage_findings
  for update to authenticated using (private.is_admin_or_manager() or private.is_support_staff())
  with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert, update on table public.damage_findings to authenticated;

create policy "Morley staff read diagnostics" on public.diagnostic_results
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create diagnostics" on public.diagnostic_results
  for insert to authenticated with check ((private.is_admin_or_manager() or private.is_support_staff()) and created_by = auth.uid());
create policy "Morley staff update diagnostics" on public.diagnostic_results
  for update to authenticated using (private.is_admin_or_manager() or private.is_support_staff())
  with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert, update on table public.diagnostic_results to authenticated;

create policy "Morley staff read valuation quotes" on public.valuation_quotes
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create valuation quotes" on public.valuation_quotes
  for insert to authenticated with check ((private.is_admin_or_manager() or private.is_support_staff()) and created_by = auth.uid());
grant select, insert on table public.valuation_quotes to authenticated;

create policy "Morley staff read risk flags" on public.deal_risk_flags
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create risk flags" on public.deal_risk_flags
  for insert to authenticated with check (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff update risk flags" on public.deal_risk_flags
  for update to authenticated using (private.is_admin_or_manager() or private.is_support_staff())
  with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert, update on table public.deal_risk_flags to authenticated;

create policy "Morley staff read overrides" on public.staff_overrides
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff append overrides" on public.staff_overrides
  for insert to authenticated with check ((private.is_admin_or_manager() or private.is_support_staff()) and actor_user_id = auth.uid());
grant select, insert on table public.staff_overrides to authenticated;

create policy "Morley staff read passports" on public.device_passports
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff create passports" on public.device_passports
  for insert to authenticated with check ((private.is_admin_or_manager() or private.is_support_staff()) and created_by = auth.uid());
create policy "Morley staff update passports" on public.device_passports
  for update to authenticated using (private.is_admin_or_manager() or private.is_support_staff())
  with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert, update on table public.device_passports to authenticated;

create policy "Morley staff read passport events" on public.device_passport_events
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff append passport events" on public.device_passport_events
  for insert to authenticated with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert on table public.device_passport_events to authenticated;

create policy "Morley staff read model versions" on public.ai_model_versions
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
grant select on table public.ai_model_versions to authenticated;

create policy "Morley staff read AI decision audit" on public.ai_decision_audit
  for select to authenticated using (private.is_admin_or_manager() or private.is_support_staff());
create policy "Morley staff append AI decision audit" on public.ai_decision_audit
  for insert to authenticated with check (private.is_admin_or_manager() or private.is_support_staff());
grant select, insert on table public.ai_decision_audit to authenticated;

commit;
