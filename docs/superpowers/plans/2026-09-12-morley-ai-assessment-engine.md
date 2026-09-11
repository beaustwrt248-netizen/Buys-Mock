# Morley AI Assessment Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a shared, auditable Morley device assessment engine that can power staff Device Lens/Admin first and later customer intake without bypassing protected commercial/security actions.

**Architecture:** Add a provider-independent assessment domain module and Supabase persistence/audit schema. Existing Nova evidence ranking, protected pricing, Guardian and catalogue authority remain upstream/downstream contracts. UI and Nova consume the same assessment result rather than implementing parallel scoring logic.

**Tech Stack:** Browser JavaScript/ES modules where existing tests support it, Node `node:test` regression tests, Supabase/Postgres migrations and existing Morley web/Admin/Android wrappers.

**Spec:** `docs/superpowers/specs/2026-09-12-morley-ai-assessment-engine-design.md`

## Global Constraints
- V1 requires staff confirmation for final condition grade, buy price, repair disposition and stock/listing creation.
- Unknown model/storage facts block commercial completion; they are never guessed.
- No client-side service role or permission/RLS/security-policy mutation.
- Existing pricing and Guardian protected-action boundaries may not be downgraded.
- AI findings and staff overrides remain attributable and auditable.
- Model/provider failure must degrade to explicit unknown/manual review, never fabricated evidence.

---

### Task 1: Assessment domain contract and deterministic state/scoring core

**Files:**
- Create: `morley-ai-assessment-core.js`
- Create: `tests/morley-ai-assessment-core.test.mjs`

**Interfaces:**
- Produces `window.MorleyAssessmentCore` and CommonJS-free test exports through a guarded global/module-friendly factory pattern consistent with current browser scripts.
- Public functions: `normalizeEvidence(items)`, `resolveAssessmentState(input)`, `scoreCondition(input)`, `canPrepareStock(input)`, `buildAssessmentProposal(input)`.

- [ ] **Step 1: Write failing tests** covering evidence confidence clamping, identity/storage blockers, critical diagnostic grade cap, stable 0-100 scoring, explicit unknown diagnostic handling and stock-preparation blocking without staff confirmation.
- [ ] **Step 2: Run** `node --test tests/morley-ai-assessment-core.test.mjs`; expect failure because the core file does not exist.
- [ ] **Step 3: Implement minimal provider-independent core**. Condition scoring uses version `condition-v1`: cosmetic 45%, functional 55%; unknown diagnostics reduce confidence but do not count as pass; a critical functional failure caps recommended grade at `faulty`. `canPrepareStock` requires resolved identity, resolved storage, sufficient evidence and explicit confirmations for grade/buy-price/repair decision.
- [ ] **Step 4: Run** `node --test tests/morley-ai-assessment-core.test.mjs`; expect pass.
- [ ] **Step 5: Commit** `test/feat: add Morley assessment core`.

### Task 2: Supabase assessment/audit schema

**Files:**
- Create: `supabase/migrations/20260912030000_morley_ai_assessment_core.sql`
- Create: `tests/morley-ai-assessment-schema.test.mjs`

**Interfaces:**
- Produces tables: `device_assessments`, `assessment_evidence`, `damage_findings`, `diagnostic_results`, `valuation_quotes`, `deal_risk_flags`, `staff_overrides`, `device_passports`, `device_passport_events`, `ai_model_versions`, `ai_decision_audit`.
- Internal commercial fields stay staff/admin-only. Customer reuse is additive later and must not expose margin/risk/internal notes.

- [ ] **Step 1: Write failing migration contract test** asserting every required table exists in migration text, RLS is enabled, raw service-role keys are absent, commercial confirmation columns exist and audit/passport event tables are append-oriented.
- [ ] **Step 2: Run** `node --test tests/morley-ai-assessment-schema.test.mjs`; expect failure because migration is absent.
- [ ] **Step 3: Implement migration** with UUID primary keys, timestamps, constrained enum-like checks, JSONB details, indexes by assessment/passport/state, RLS enabled on all new tables, staff/admin policies using the repository's existing role helper patterns, and no broad anonymous writes.
- [ ] **Step 4: Run** schema contract test; expect pass.
- [ ] **Step 5: Run existing auth/security migration tests** discovered in `tests/` and correct any boundary regression.
- [ ] **Step 6: Commit** `feat: add assessment persistence and audit schema`.

### Task 3: Device Lens/Admin assessment client

**Files:**
- Create: `morley-ai-assessment-client.js`
- Create: `tests/morley-ai-assessment-client.test.mjs`
- Modify the existing Device Lens/Admin loader only after locating its canonical script-registration point.

**Interfaces:**
- Consumes `MorleyAssessmentCore`, current auth session and approved Supabase RPC/table surface.
- Produces `createAssessment`, `addEvidence`, `recordDiagnostic`, `requestProposal`, `confirmCommercialDecision`, `prepareStockPayload`.

- [ ] **Step 1: Write failing tests** for auth requirement, masked identifier handling, structured recoverable errors and refusal to prepare stock when `canPrepareStock` is false.
- [ ] **Step 2: Run targeted test** and verify failure.
- [ ] **Step 3: Implement minimal client** with explicit `status`, `code`, `recoverable` errors; never persist full identifiers to localStorage/logs.
- [ ] **Step 4: Register script in the canonical staff surface without duplicating loaders.**
- [ ] **Step 5: Run targeted + loader/auth parity tests.**
- [ ] **Step 6: Commit** `feat: connect staff assessment client`.

### Task 4: Guided capture and damage overlay contract

**Files:**
- Create: `morley-ai-damage-overlay.js`
- Create: `tests/morley-ai-damage-overlay.test.mjs`
- Modify Device Lens view script discovered during Task 3.

**Interfaces:**
- Consumes normalized image dimensions and findings `{type,severity,confidence,region:{x,y,width,height}}` with all coordinates 0..1.
- Produces validated overlay geometry and capture completeness `{front,rear,additional,qualityIssues}`.

- [ ] **Step 1: Write tests** for coordinate clamping/rejection, front+rear minimum evidence and blur/glare retake state.
- [ ] **Step 2: Run and verify failure.**
- [ ] **Step 3: Implement geometry/capture normalizer** independent of model provider.
- [ ] **Step 4: Add staff overlay/review UI** with accept/reject/amend controls; original AI finding stays in audit data.
- [ ] **Step 5: Run targeted/browser-contract tests.**
- [ ] **Step 6: Commit** `feat: add guided damage review contract`.

### Task 5: Diagnostics and Morley Condition Score integration

**Files:**
- Modify: `morley-ai-assessment-core.js`
- Modify the existing diagnostics/Device Lens integration point discovered in repository search.
- Create: `tests/morley-ai-condition-integration.test.mjs`

**Interfaces:**
- Diagnostic status is exactly `pass|fail|unknown|not_tested`.
- Critical failures include configured display/touch/charging/activation-lock identity blockers and cap commercial recommendation appropriately.

- [ ] **Step 1: Add failing integration fixtures** for full-pass, cosmetic damage, partial unknown and critical failure.
- [ ] **Step 2: Verify failure.**
- [ ] **Step 3: Wire normalized diagnostics into `scoreCondition`.**
- [ ] **Step 4: Render cosmetic, functional, overall and confidence separately.**
- [ ] **Step 5: Run assessment/diagnostic regression suites.**
- [ ] **Step 6: Commit** `feat: integrate condition scoring`.

### Task 6: Valuation, repair recommendation and guardrails

**Files:**
- Create: `morley-ai-commercial-proposal.js`
- Create: `tests/morley-ai-commercial-proposal.test.mjs`
- Integrate with existing protected pricing preview path, not direct table mutation.

**Interfaces:**
- Produces `{baseMarketValue,adjustments,repairEstimate,targetResale,proposedBuy,expectedMargin,confidence,recommendation,explanation,rulesVersion}`.
- Recommendation is `buy_repair|buy_as_is|parts_only|review` in v1.

- [ ] **Step 1: Write failing tests** proving margin floor/maximum-buy rules dominate AI proposal, unknown identity/storage blocks quote completion and repair recommendation changes when repair destroys margin.
- [ ] **Step 2: Verify failure.**
- [ ] **Step 3: Implement deterministic commercial calculator around protected pricing inputs.**
- [ ] **Step 4: Add factor-level explanation and confidence propagation.**
- [ ] **Step 5: Run pricing security/parity tests plus targeted tests.**
- [ ] **Step 6: Commit** `feat: add guarded commercial proposals`.

### Task 7: Deal protection and Device Passport

**Files:**
- Create: `morley-ai-risk.js`
- Create: `morley-device-passport.js`
- Create: `tests/morley-ai-risk-passport.test.mjs`

**Interfaces:**
- Risk checks return review flags only; high severity forces `review_required` but never automatic accusation/rejection.
- Passport events are append-only normalized events with actor/source/version metadata.

- [ ] **Step 1: Write failing duplicate/evidence-mismatch/price-anomaly tests and passport ordering/audit tests.**
- [ ] **Step 2: Verify failure.**
- [ ] **Step 3: Implement deterministic risk rules and append-only event builder.**
- [ ] **Step 4: Connect risk state to assessment state machine.**
- [ ] **Step 5: Run targeted + auth tests.**
- [ ] **Step 6: Commit** `feat: add deal protection and device passport`.

### Task 8: Protected stock preparation and one-photo-to-stock review

**Files:**
- Create: `morley-ai-stock-prep.js`
- Create: `tests/morley-ai-stock-prep.test.mjs`
- Modify existing inventory/catalogue picker integration only at its canonical protected write path.

**Interfaces:**
- Produces a stock draft only after `canPrepareStock` passes.
- Final creation remains an existing protected staff action.

- [ ] **Step 1: Write failing tests** for unresolved identity/storage, missing confirmations, risk review and complete approved flow.
- [ ] **Step 2: Verify failure.**
- [ ] **Step 3: Build stock draft mapper** for catalogue link, condition, photos, proposed description/resale and label metadata.
- [ ] **Step 4: Add review UI immediately before protected stock creation.**
- [ ] **Step 5: Run inventory/catalogue integrity tests.**
- [ ] **Step 6: Commit** `feat: add reviewed stock preparation`.

### Task 9: Nova permissioned assessment tools

**Files:**
- Modify canonical Nova capability registry discovered in `nova/` or Admin core.
- Create: `tests/nova-assessment-capabilities.test.mjs`

**Interfaces:**
- Read intents: assessment status/explanation/exceptions/calibration/repair opportunities.
- Mutation intents delegate to existing assessment confirmation APIs and preserve risk class.

- [ ] **Step 1: Write failing capability registration/risk-class tests.**
- [ ] **Step 2: Verify failure.**
- [ ] **Step 3: Register read tools and protected mutation tools.**
- [ ] **Step 4: Prove natural-language routing cannot downgrade protected actions.**
- [ ] **Step 5: Run Nova + Guardian authority-boundary tests.**
- [ ] **Step 6: Commit** `feat: expose assessment tools to Nova`.

### Task 10: Restricted customer reuse and release gates

**Files:**
- Create customer assessment adapter at the existing Morley Buys intake integration point.
- Create: `tests/morley-ai-customer-boundary.test.mjs`
- Update release/readiness documentation.

**Interfaces:**
- Customer can create/update own permitted intake evidence and read customer-safe assessment status only.
- Internal margin, risk heuristics, staff notes, protected identifiers and admin diagnostics are never returned.

- [ ] **Step 1: Write customer-boundary tests** before exposing the adapter.
- [ ] **Step 2: Implement restricted adapter.**
- [ ] **Step 3: Run full security, parity, feature-contract and quality suites.**
- [ ] **Step 4: Build/verify Android staff/customer surfaces where affected.**
- [ ] **Step 5: Open PR from `feat/morley-ai-assessment-engine` to `main` with test evidence; do not merge while required checks are failing.**
