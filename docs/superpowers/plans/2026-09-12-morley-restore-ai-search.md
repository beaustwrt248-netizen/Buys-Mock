# Morley Restore, AI Scan History, Repair-or-Buy and Search Modes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make major Morley deployments recoverable, persist every Device Lens scan, expose staff Repair-or-Buy decisions, and give each search entry point a distinct workflow over the shared Universal Buy Search engine.

**Architecture:** Extend the existing Morley assessment domain and deployment governance instead of creating parallel systems. Restore points index immutable source/release/provider state and use protected preview-first rollback; Device Lens creates and checkpoints the existing assessment entity; Repair-or-Buy reuses versioned core rules; search modes alter orchestration while sharing catalogue/pricing authority.

**Tech Stack:** GitHub Actions/Git history/releases, JavaScript, Kotlin/Jetpack Compose, Supabase PostgreSQL/RLS/Storage/Edge Functions, existing Morley assessment/pricing/Guardian/Nova contracts.

**Spec:** `docs/superpowers/specs/2026-09-12-morley-restore-ai-search-design.md`

## Global Constraints
- Preserve existing Auth/RLS, Guardian, pricing, signing and protected release/rollback approval boundaries.
- No client-side service role and no credentials/secrets in restore manifests.
- Production-data rollback is never automatic; database recovery requires reviewed forward-safe recovery/compensating migration.
- AI recommendations remain advisory and require staff confirmation for commercial actions.
- Failed, cancelled and incomplete AI scans are retained.
- Universal Buy Search remains the shared catalogue/matching/pricing engine.
- Each production slice must pass existing security, feature-contract, quality, parity and relevant Android/release gates.

---

### Task 1: Restore-point domain contract and governance

**Files:**
- Create: `morley-restore-core.js`
- Create: `tests/morley-restore-core.test.mjs`
- Create: `supabase/migrations/<timestamp>_morley_restore_points.sql`
- Modify: `.github/workflows/quality-gate.yml`

**Interfaces:**
- Produces: `buildRestoreManifest(input)`, `validateRestoreManifest(manifest)`, `previewComponentRestore(current, target, components)`.
- Manifest fields: `sourceSha`, `changeRef`, `components[]`, `releaseRefs`, `webRefs`, `functionRefs`, `migrationHead`, `configFingerprints`, `createdAt`; secret-like keys are rejected.

- [ ] Write failing JS contract tests asserting immutable normalized manifests, SHA/component validation, secret-key rejection, component-only preview and a hard `database_recovery_requires_protected_plan` blocker.
- [ ] Run `node --test tests/morley-restore-core.test.mjs`; verify RED because the core does not exist.
- [ ] Implement `morley-restore-core.js` as deterministic pure functions with no deployment authority.
- [ ] Run the test again and verify GREEN.
- [ ] Write migration contract coverage asserting append-only `restore_points` and `restore_events`, RLS using existing staff/Admin role helpers, and no generic destructive database-restore RPC.
- [ ] Add the migration with immutable restore metadata and append-only restore audit events; grant only the minimum role-specific access used by Admin.
- [ ] Run schema/security/quality tests and verify GREEN.
- [ ] Commit as `feat: add Morley restore point contracts`.

### Task 2: Capture restore points in major deployment workflows

**Files:**
- Create: `scripts/build-restore-manifest.mjs`
- Create: `tests/restore-workflow-contract.test.mjs`
- Modify: relevant production deployment workflows under `.github/workflows/` for Morley Buys web/APK/OTA, Admin, Nova and Supabase deployment paths.

**Interfaces:**
- Consumes: `buildRestoreManifest` contract from Task 1.
- Produces: a pre-deployment restore manifest artifact/record and post-deployment verification status.

- [ ] Write failing workflow tests requiring a restore capture before deploy/release jobs and forbidding secret values in manifest arguments/output.
- [ ] Run the targeted Node workflow contract; verify RED against current workflows.
- [ ] Implement manifest generation from Git SHA, version/release metadata, affected component classification and provider/version references already available to each workflow.
- [ ] Make deployment depend on successful restore capture; add post-deploy health verification that records `verified` or `deployment_failed` without silently rolling back data.
- [ ] Run workflow/security/quality contracts and verify GREEN.
- [ ] Commit as `feat: capture restore points before Morley deployments`.

### Task 3: Admin Backups & Restore read/preview surface

**Files:**
- Create: `admin/restore-centre.js`
- Modify: Admin navigation/home registration files discovered from current main before implementation.
- Create: `tests/admin-restore-centre.test.mjs`

**Interfaces:**
- Consumes: `restore_points`, `restore_events`, `previewComponentRestore`.
- Produces: history list, detail view and preview request; no direct unconfirmed restore.

- [ ] Write failing Admin contract tests for `Backups & Restore`, restore-point status/components, preview-first action, database warning and explicit approval requirement.
- [ ] Run the targeted Admin test; verify RED.
- [ ] Implement a focused Admin module using the existing authenticated Supabase client and Admin component patterns.
- [ ] Add protected restore preparation only; execution must remain behind existing protected-action approval/audit mechanisms.
- [ ] Run Admin integration, UI checklist, security, parity and quality gates; verify GREEN.
- [ ] Commit as `feat: add Admin backup and restore centre`.

### Task 4: Durable Device Lens scan session and checkpoints

**Files:**
- Modify: `morley-ai-assessment-client.js`
- Modify: `android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt`
- Create focused Android persistence helper if needed after inspecting current networking/storage patterns.
- Modify/Create: assessment client and Android regression tests.
- Add a forward migration only if existing assessment columns cannot represent capture/cancel/failure checkpoints.

**Interfaces:**
- Produces: `createAssessment({state:'capture_started', source:'device_lens'})`, `checkpointAssessment(id, checkpoint)`, local pending-sync envelope keyed by assessment ID.

- [ ] Write failing JS tests requiring assessment creation at scan start plus checkpoint states `front_captured`, `rear_captured`, `analysis_failed`, `review_ready`, `cancelled`, `completed`.
- [ ] Write failing Android contract test proving Device Lens creates a durable scan session before first capture and does not delete durable history on close/reset.
- [ ] Run targeted tests and verify RED.
- [ ] Implement assessment checkpoint methods with authenticated writes and sanitized metadata.
- [ ] Wire Device Lens to create/checkpoint the assessment while retaining local photo files only as temporary caches.
- [ ] Add bounded local pending-sync persistence for temporary network failure and retry on resumed connectivity/session; commercial completion must show unsynced state until durable persistence succeeds.
- [ ] Run JS tests, Android unit tests and lint; verify GREEN.
- [ ] Commit as `feat: persist every Device Lens scan`.

### Task 5: Protected image evidence persistence and AI Scan History

**Files:**
- Add/modify Supabase migration/storage policy for assessment evidence paths.
- Create Android `AI Scan History` screen/module and focused data client.
- Modify navigation registration to expose `AI Scan History` in the appropriate History/More surface.
- Add JS/schema/Android tests.

**Interfaces:**
- Produces: `listScanHistory(filters)`, `getScanHistory(id)`, `resumeScan(id)`, protected evidence storage references.

- [ ] Write failing schema/RLS tests proving staff can access authorised assessment evidence while raw identifiers/secrets are excluded and unauthorised users cannot enumerate scans.
- [ ] Write failing Android UI/domain tests for completed/incomplete/failed/cancelled rows and resume/retry actions.
- [ ] Implement protected evidence upload/reference policy and history queries using existing assessment/evidence tables.
- [ ] Implement history/detail/resume UI without delete controls.
- [ ] Run schema security, Android, feature-contract and parity gates; verify GREEN.
- [ ] Commit as `feat: add persistent AI scan history`.

### Task 6: Complete Repair-or-Buy rules and persistence

**Files:**
- Modify: `morley-ai-assessment-core.js`
- Modify: `morley-ai-assessment-client.js`
- Modify: core/client tests.

**Interfaces:**
- `decideRepairStrategy(input)` returns `buy_and_repair | buy_as_is | parts_only | review_required`, explanation, margins/costs/timing, advisory authority and staff-confirmation requirement.

- [ ] Add failing fixtures for buy-and-repair, buy-as-is, parts-only, below-margin review, missing/unverified commercial inputs and deterministic tie handling.
- [ ] Run `node --test` for assessment core/client; verify RED for parts-only/validation behavior.
- [ ] Extend the existing versioned repair rules minimally; do not introduce model-driven pricing authority.
- [ ] Persist proposal/audit/passport repair event through existing assessment structures; keep final decision in `confirmCommercialDecision` with explicit staff confirmation.
- [ ] Run core/client/security tests; verify GREEN.
- [ ] Commit as `feat: complete Repair or Buy decision rules`.

### Task 7: Device Lens Repair-or-Buy staff UI

**Files:**
- Modify: `android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt`
- Create a focused Compose module such as `MorleyRepairDecisionUi.kt` if the activity would otherwise grow further.
- Add Android regression tests.

**Interfaces:**
- Consumes verified assessment, pricing and `decideRepairStrategy` output.
- Produces staff-confirmed repair disposition checkpoint/passport event.

- [ ] Write failing Android tests for recommendation card, as-is/repaired margin evidence, repair cost/timing, four recommendation states and explicit staff confirmation.
- [ ] Run targeted Android tests; verify RED.
- [ ] Add a Repair Decision step after pricing/condition evidence and before stock preparation.
- [ ] Show factor-level reasoning and distinguish AI recommendation from staff-confirmed decision.
- [ ] Block stock preparation until the existing confirmation/evidence gate is satisfied.
- [ ] Run Android unit tests/lint and parity gates; verify GREEN.
- [ ] Commit as `feat: add Device Lens Repair or Buy workflow`.

### Task 8: Universal Buy Search mode contract

**Files:**
- Locate current Universal Buy Search Android/web implementation on current main before editing.
- Create/modify a small shared mode contract rather than duplicating search logic.
- Add routing/search contract tests.

**Interfaces:**
- Modes: `quick_search`, `manual_search`, `price_check`, `ai_scan`.
- All non-camera modes consume the same catalogue/matching/pricing services; `ai_scan` routes to Device Lens while sharing resolved catalogue/pricing contracts downstream.

- [ ] Write failing tests proving each entry point supplies the correct mode and that no mode forks protected pricing/catalogue authority.
- [ ] Run targeted routing tests; verify RED.
- [ ] Implement mode parsing/defaulting and preserve deep-link/intent mode across navigation.
- [ ] Run tests; verify GREEN.
- [ ] Commit as `feat: add Universal Buy Search modes`.

### Task 9: Give each search entry point a distinct workflow

**Files:**
- Modify dashboard/search/navigation and Universal Buy Search presentation files identified in Task 8.
- Add Android/web/mobile-web parity tests.

**Interfaces:**
- Search bar -> `quick_search` live suggestions/results.
- Manual Search -> guided category/brand/model/storage selection.
- Price Check -> identification then condition/valuation path.
- AI Scan -> Device Lens camera flow and Repair-or-Buy.

- [ ] Add failing navigation/UI tests for the four distinct entry-point outcomes.
- [ ] Run targeted tests; verify RED.
- [ ] Implement presentation/orchestration differences while keeping one shared result/pricing engine.
- [ ] Verify browser widths and Android navigation parity with no duplicate hidden renderer.
- [ ] Run UI checklist, mobile-web theme, feature-contract, Android and ultimate parity gates; verify GREEN.
- [ ] Commit as `feat: specialize Morley search entry points`.

### Task 10: Cross-app restore verification and protected rollout

**Files:**
- Modify release/operations documentation and restore health-check tests only as required by implemented slices.

**Interfaces:**
- Consumes restore point IDs/manifests from deployed slices.
- Produces verified recovery evidence for Morley Buys, Admin, Nova/Guardian-related deployment surfaces and Supabase components.

- [ ] Create a restore point for the first production slice and verify its source/release/function/schema references resolve before deployment.
- [ ] Run the complete repository security, feature-contract, quality, Admin, Android, distribution, OTA and parity gates relevant to changed files.
- [ ] Deploy slices in dependency order only when exact-head gates are green.
- [ ] After each deployment, run health checks and mark its restore point verified; if a health check fails, stop subsequent rollout and use preview-first component rollback rather than database rewind.
- [ ] Verify live release/OTA/web/function state from authoritative sources before claiming completion.
- [ ] Commit documentation/evidence as `docs: verify Morley restore protected rollout`.
