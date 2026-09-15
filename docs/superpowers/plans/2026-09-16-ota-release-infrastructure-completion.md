# OTA and Release Infrastructure Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close demonstrated Android OTA/release-infrastructure gaps for Morley Buys, Nova Next, and Morley Admin without publishing a test-only release or rewriting verified history.

**Architecture:** Keep three independent release lanes and their existing signing/package/feed identities. Add narrow repository-enforced contracts first, then make only the minimum workflow/runtime/documentation changes required for exact-source provenance, signer/checksum/version integrity, protected promotion, updater validation, and forward-only recovery.

**Tech Stack:** GitHub Actions YAML, Node.js contract tests, Android/Gradle, shell release tooling, JSON OTA metadata.

**Spec:** `docs/superpowers/specs/2026-09-16-ota-release-infrastructure-completion-design.md`

## Global Constraints

- Morley Buys 2.15.107 / versionCode 151 is an existing verified production baseline and must not be republished for this audit.
- Nova Next 0.1.1 / versionCode 2 is an existing verified production baseline and must not be republished for this audit.
- `nova-next-v0.1.0` / versionCode 1 remains provenance-conflicted/quarantined and must never be rewritten, reused, moved, or deleted.
- Preserve all existing production signing identities, package identities, OTA feed identities, protected-main requirements, Guardian boundaries, Auth/RLS boundaries, pricing authority, and production-data authority.
- No direct protected-main writes, signing-key rotation, branch-protection weakening, historical release/tag rewriting, or unnecessary production publication.
- All lanes fail closed on signer mismatch, checksum mismatch, stale source, invalid version sequencing, conflicting immutable release, missing artifact, malformed metadata, or release-identity drift.
- Recovery is forward-only by default: use a newer corrective release or an existing explicitly validated recovery path; do not rewrite immutable history to simulate rollback.

---

### Task 1: Morley Buys release-lane contract audit and gap closure

**Files:**
- Inspect/modify only demonstrated Morley release workflow, updater, recovery, and contract files discovered from current `main`.
- Test: extend the existing Morley Android OTA/release policy contracts rather than creating a duplicate framework.

**Interfaces:**
- Consumes: current `auto-ota-release.yml`, Morley production feed contract, current Android updater/signing checks.
- Produces: repository evidence for exact-source provenance, expected signer, immutable asset reuse/refusal, SHA-256 binding, monotonic sequencing, protected promotion, stale/conflict refusal, updater discovery/install validation, and recovery semantics.

- [ ] **Step 1: Inventory the current lane**

Record exact workflow/test/runtime paths and map each completion invariant to existing evidence or a demonstrated gap. Do not edit production metadata.

- [ ] **Step 2: Write failing regression contracts for demonstrated gaps only**

Add focused assertions to existing contract suites for every missing invariant. Tests must use fixtures/static workflow inspection and must not dispatch or publish a release.

- [ ] **Step 3: Run focused contracts and capture RED evidence**

Run the narrow Morley OTA/release tests and verify each new assertion fails for the intended missing guarantee rather than an unrelated setup problem.

- [ ] **Step 4: Implement minimum fail-closed protections**

Change only the workflow/runtime/recovery files needed to satisfy the failing contracts while preserving version 2.15.107 / 151 and the current signer/feed identity.

- [ ] **Step 5: Verify GREEN and lane gates**

Run focused OTA/release contracts, Android release-policy checks, relevant lint/unit checks if Android source changed, repository security audit, quality gate, parity/feature-contract gates, and diff/whitespace validation.

- [ ] **Step 6: Commit**

Commit as `hardening: complete Morley OTA release contracts`.

---

### Task 2: Nova Next release-lane contract audit and gap closure

**Files:**
- Inspect/modify only demonstrated Nova Next production release workflow, metadata contract, native updater/manual-update UX, recovery, and associated tests discovered from current `main`.

**Interfaces:**
- Consumes: Nova Next 0.1.1 / versionCode 2 production baseline, explicit versionCode-1 quarantine, existing exact-source tag binding and signer checks.
- Produces: non-publishing evidence for all completion invariants while preserving the existing 0.1.1 release and quarantine.

- [ ] **Step 1: Inventory the current Nova lane**

Map exact-source provenance, signing, immutable release behavior, checksum metadata, sequencing/quarantine, protected feed promotion, stale-source refusal, updater discovery/install validation, manual update UX, and recovery to concrete current files/tests.

- [ ] **Step 2: Write failing non-publishing regression contracts for demonstrated gaps**

Use static workflow/metadata/runtime fixtures. Explicitly assert that versionCode 1 remains quarantined and that no test requires creation or mutation of a GitHub release/tag/feed.

- [ ] **Step 3: Run focused Nova contracts and capture RED evidence**

Verify failures correspond only to missing completion guarantees.

- [ ] **Step 4: Implement minimum protections**

Patch only demonstrated gaps. Do not mutate `nova-next-v0.1.0`, do not reuse versionCode 1, do not mint a new Nova production version, and do not alter the production signer.

- [ ] **Step 5: Verify GREEN and Nova gates**

Run the complete Nova Next contract suite, JavaScript syntax checks where relevant, isolated Android identity, lint/unit/debug build when Android source changed, repository security audit, quality gate, parity gate, and release-policy checks.

- [ ] **Step 6: Commit**

Commit as `hardening: complete Nova Next OTA release contracts`.

---

### Task 3: Morley Admin release-lane contract audit and gap closure

**Files:**
- Inspect/modify the existing Admin Android build, release-check, OTA-feed-deploy, recovery workflows/runtime, and associated contract tests discovered from current `main`.

**Interfaces:**
- Consumes: Admin's existing package/signing/feed identity and dedicated build/release-check/feed-deploy/recovery workflows.
- Produces: equivalent fail-closed guarantees to Morley/Nova without replacing Admin's release lane or publishing an audit-only build.

- [ ] **Step 1: Inventory the current Admin lane**

Map all completion invariants to current Admin workflow/runtime/test evidence and identify only concrete gaps.

- [ ] **Step 2: Write failing contracts for each demonstrated Admin gap**

Prefer extending Admin release/recovery contracts. Validate exact source, signer, immutable identity, checksum, monotonic version, protected feed promotion, stale/conflict refusal, updater discovery/install behavior, and forward-only recovery.

- [ ] **Step 3: Run focused Admin contracts and capture RED evidence**

Confirm intended failures before implementation.

- [ ] **Step 4: Implement minimum Admin protections**

Preserve Admin package, signer, feed, and production version identity. Do not publish a new Admin release solely for this audit.

- [ ] **Step 5: Verify GREEN and Admin gates**

Run focused Admin OTA/release/recovery contracts, Android validation if source changed, Admin control integration/release readiness gates, repository security audit, quality/parity gates, and diff validation.

- [ ] **Step 6: Commit**

Commit as `hardening: complete Admin OTA release contracts`.

---

### Task 4: Cross-lane recovery documentation and completion evidence

**Files:**
- Create/modify the narrow release/recovery documentation and a cross-lane static completion contract if the repository lacks one.
- Do not centralize runtime release authority.

**Interfaces:**
- Consumes: evidence produced by Tasks 1–3.
- Produces: one auditable matrix showing each app has all required invariants and documented forward-only recovery semantics.

- [ ] **Step 1: Write a failing cross-lane completion contract**

The contract must identify Morley Buys, Nova Next, and Morley Admin independently and require evidence paths for provenance, signer, immutable artifact, checksum, sequencing, protected promotion, stale/conflict refusal, updater validation, and recovery.

- [ ] **Step 2: Run it and capture RED evidence**

Verify any failure is an evidence/documentation gap, not an attempt to couple release identities.

- [ ] **Step 3: Add the minimum recovery/evidence documentation**

Document forward-only correction, immutable-history preservation, feed safety, signer/checksum failure handling, stale-source refusal, and app-specific recovery entry points. Preserve each lane's independent authority.

- [ ] **Step 4: Run complete verification**

Run all three app-specific release/OTA contract suites, the cross-lane completion contract, repository security audit, quality gate, applicable parity/feature-contract gates, release-readiness/recovery gates, and exact-head CI status review.

- [ ] **Step 5: Review production state without publishing**

Confirm current verified production releases/feeds/tags remain unchanged by the audit. Any genuine future release remains a separately approved protected action.

- [ ] **Step 6: Commit**

Commit as `docs: complete OTA recovery and release evidence`.

---

## Self-review

- Spec coverage: Tasks 1–3 independently cover every required invariant for each active Android lane; Task 4 verifies cross-lane evidence and recovery without centralizing release authority.
- Placeholder scan: no TBD/TODO/implement-later steps; implementation is intentionally conditioned on *demonstrated* gaps because the approved spec forbids unnecessary changes and the exact current file paths must be discovered from current `main` before edits.
- Type/interface consistency: all tasks produce static repository evidence and preserve app-specific identities; Task 4 consumes only those evidence paths and does not create a shared release identity.
