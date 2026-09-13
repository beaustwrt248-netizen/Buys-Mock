# Nova Next Promotion Readiness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a verified go/no-go report for replacing current Nova, without performing any protected cutover.

**Architecture:** Build the report from repository contracts, live deployment checks, Android validation and a feature-parity matrix. Any production route/package/signing/release action is explicitly excluded and listed as a protected next step.

**Tech Stack:** Repository tests, GitHub Actions evidence, live Pages smoke evidence, Android build artifacts, Markdown report.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- Audit only; no production route cutover, package/signing change, release or OTA.
- Current Nova remains available and unchanged.
- Every readiness claim must cite fresh test/build/live evidence.
- Unknown or unverified items are blockers, not assumed successes.

---

### Task 1: Feature parity matrix
**Files:**
- Create: `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md`

- [ ] **Step 1:** Inventory current Nova user-visible capability groups and approved Nova Next target capabilities.
- [ ] **Step 2:** Record each row as `ready`, `ready-with-boundary`, `blocked`, or `not-applicable`, with exact evidence/test/route.
- [ ] **Step 3:** Include auth, chat, knowledge/research, vision, code proposals, files, tasks, projects, calendar, product search, voice, automation, integrations, help/settings and Android wrapper.

### Task 2: Security/privacy/accessibility evidence
- [ ] **Step 1:** Run complete Nova Next tests and repository security/parity/quality checks on the final candidate head.
- [ ] **Step 2:** Verify no client bundle contains privileged credentials and no protected function is client-allowlisted beyond approved safe contracts.
- [ ] **Step 3:** Verify local/session persistence boundaries for auth, files, workspace, preferences and automation.
- [ ] **Step 4:** Record accessibility/responsive contract results and any manual mobile/WebView observations.

### Task 3: Deployment/cache/mobile evidence
- [ ] **Step 1:** Verify `https://buyshub.me/nova-next/` and core static assets are live from the final candidate.
- [ ] **Step 2:** Verify `/nova/` remains independently available.
- [ ] **Step 3:** Verify service-worker scope/cache names remain isolated to `/nova-next/`.
- [ ] **Step 4:** Record Android debug build result and SHA-256.

### Task 4: Go/no-go decision
- [ ] **Step 1:** Summarize blockers and classify each as low/medium follow-up or protected cutover dependency.
- [ ] **Step 2:** State `GO FOR PROMOTION REVIEW` only if all non-protected completion requirements are verified; otherwise state `NO-GO` with exact blockers.
- [ ] **Step 3:** List protected next actions separately: production web route replacement, Android production application identity/signing/version compatibility, release/OTA, and any backend authority expansion.
- [ ] **Step 4:** Commit report and merge documentation-only PR after green checks; do not perform protected next actions without exact approval.
