-- Cover advisor-confirmed foreign keys with leading B-tree indexes.
-- Performance-only hardening: no data, RLS, grant, function, or constraint changes.

create index if not exists ai_decision_audit_actor_user_id_idx
  on public.ai_decision_audit (actor_user_id);

create index if not exists assessment_evidence_created_by_idx
  on public.assessment_evidence (created_by);

create index if not exists damage_findings_evidence_id_idx
  on public.damage_findings (evidence_id);

create index if not exists damage_findings_reviewed_by_idx
  on public.damage_findings (reviewed_by);

create index if not exists deal_risk_flags_resolved_by_idx
  on public.deal_risk_flags (resolved_by);

create index if not exists device_assessments_buy_price_confirmed_by_idx
  on public.device_assessments (buy_price_confirmed_by);

create index if not exists device_assessments_grade_confirmed_by_idx
  on public.device_assessments (grade_confirmed_by);

create index if not exists device_assessments_repair_decision_confirmed_by_idx
  on public.device_assessments (repair_decision_confirmed_by);

create index if not exists device_passport_events_actor_user_id_idx
  on public.device_passport_events (actor_user_id);

create index if not exists device_passport_events_assessment_id_idx
  on public.device_passport_events (assessment_id);

create index if not exists device_passports_created_by_idx
  on public.device_passports (created_by);

create index if not exists diagnostic_results_created_by_idx
  on public.diagnostic_results (created_by);

create index if not exists nova_knowledge_ingestion_runs_created_by_idx
  on public.nova_knowledge_ingestion_runs (created_by);

create index if not exists staff_overrides_actor_user_id_idx
  on public.staff_overrides (actor_user_id);

create index if not exists valuation_quotes_created_by_idx
  on public.valuation_quotes (created_by);
