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

## Protected approval
- On 2026-09-12, Beau explicitly approved the protected schema migration, final merge and release/version promotion, subject to all required repository gates completing successfully.
- This approval does not waive security, RLS, Guardian, pricing, release-signing or OTA monotonic-version checks.

---

### Task 1: Assessment domain contract and deterministic state/scoring core

**Files:**
- Create: `morley-ai-assessment-core.js`
- Create: `tests/morley-ai-assessment-core.test.mjs`

**Interfaces:**
- Produces `window.MorleyAssessmentCore` and CommonJS-free test exports through a guarded global/module-friendly factory pattern consistent with current browser scripts.
- Public functions: `normalizeEvidence(items)`, `resolveAssessmentState(input)`, `scoreCondition(input)`, `canPrepareStock(input)`, `buildAssessmentProposal(input)`.

- [x] **Step 1: Write failing tests** covering evidence confidence clamping, identity/storage blockers, critical diagnostic grade cap, stable 0-100 scoring, explicit unknown diagnostic handling and stock-preparation blocking without staff confirmation.
- [x] **Step 2: Run** `node --test tests/morley-ai-assessment-core.test.mjs`; verified RED before implementation.
- [x] **Step 3: Implement minimal provider-independent core**. Condition scoring uses version `condition-v1`: cosmetic 45%, functional 55%; unknown diagnostics reduce confidence but do not count as pass; a critical functional failure caps recommended grade at `faulty`. `canPrepareStock` requires resolved identity, resolved storage, sufficient evidence and explicit confirmations for grade/buy-price/repair decision.
- [x] **Step 4: Run** targeted contract; verified GREEN.

### Task 2: Supabase assessment/audit schema

**Files:**
- Create: `supabase/migrations/20260912030000_morley_ai_assessment_core.sql`
- Create: `tests/morley-ai-assessment-schema.test.mjs`

**Interfaces:**
- Produces tables: `device_assessments`, `assessment_evidence`, `damage_findings`, `diagnostic_results`, `valuation_quotes`, `deal_risk_flags`, `staff_overrides`, `device_passports`, `device_passport_events`, `ai_model_versions`, `ai_decision_audit`.
- Internal commercial fields stay staff/admin-only. Customer reuse is additive later and must not expose margin/risk/internal notes.

- [x] Migration contract, RLS/table contract, security audit and schema CI verified on the isolated PR branch.
- [ ] Apply approved migration to production only after the final branch validation is green.

### Task 3: Device Lens/Admin assessment client

**Files:**
- Create: `morley-ai-assessment-client.js`
- Modify existing Android Device Lens integration through `MorleyAssessmentBridge.kt` and `MorleyVisionReviewUi.kt`.

**Interfaces:**
- Authenticated Supabase access only.
- Produces assessment/evidence/diagnostic persistence and commercial-confirmation helpers.
- Strips raw IMEI/serial fields from persistence payloads.
- Refuses stock preparation unless the shared core authorises it.

- [x] Client contract written RED-first.
- [x] Authenticated persistence client implemented.
- [ ] Final CI verification on the current release-identity commit.

### Task 4: Guided capture and damage review

- [x] Existing Device Lens front/rear evidence capture retained.
- [x] Existing fitted-image damage-region renderer retained.
- [x] Confirmed/dismissed/pending damage findings promoted into structured assessment evidence.
- [x] Damage review contract verified.

### Task 5: Diagnostics and Morley Condition Score integration

- [x] Existing nine staff checks reused: Display, Touch, Camera, Speaker, Microphone, Charging, Buttons, Vibration, Connectivity.
- [x] Unsupported/unavailable remain non-passing.
- [x] Deterministic Condition Score adapter implemented and contract-verified.
- [ ] Full rendered Admin condition presentation remains limited to verified evidence; no synthetic cosmetic score is permitted.

### Task 6: Valuation, repair recommendation and guardrails

- [x] Deterministic advisory valuation implemented.
- [x] Hard max-buy and minimum-margin rules dominate model recommendations.
- [x] Evidence blockers prevent quote completion.
- [x] Repair-vs-as-is recommendation implemented as advisory only.
- [x] Contract verified RED→GREEN.

### Task 7: Deal protection and Device Passport

- [x] Review-only deterministic risk flags implemented.
- [x] Raw identifiers excluded from risk output; protected identifier references may be retained.
- [x] Typed, append-only Device Passport event builder implemented.
- [x] Cross-VM test-harness assertion defect root-caused and corrected without changing production logic.
- [x] Contract verified GREEN.

### Task 8: Protected stock preparation

- [x] Stock draft orchestration implemented behind existing evidence/risk/staff-confirmation gates.
- [x] Final stock creation/publish remains a protected staff action.
- [x] Customer-safe assessment projection implemented without internal margin/risk exposure.
- [x] Contract verified.

### Task 9: Nova permissioned assessment tools

- [x] Shared command-planning/risk-class contract implemented and verified.
- [ ] Canonical Nova registry exposure must preserve the existing action classes and Guardian approval boundary.

### Task 10: Release gates

- [x] Repository Security Audit verified on prior feature commits.
- [x] B&L Morley Quality Gate verified on prior feature commits.
- [x] Morley Ultimate Parity Gate verified on prior feature commits.
- [x] Admin Control Integration Audit verified on prior feature commits.
- [x] Full Feature Contract Audit verified on prior feature commits.
- [x] Android source changes detected by OTA policy as requiring a fresh identity.
- [x] Exact next Android identity minted: `2.15.96` / `versionCode 140` from main `2.15.95` / `139`.
- [ ] Wait for fresh required CI on the exact release commit.
- [ ] Apply approved Supabase migration.
- [ ] Mark PR ready and merge only after required checks are green.
- [ ] Promote only a signed/checksummed APK and matching OTA metadata.
