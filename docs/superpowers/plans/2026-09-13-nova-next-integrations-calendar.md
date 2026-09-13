# Nova Next Integrations and Calendar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve local calendar usefulness and truthful integration visibility without collecting third-party credentials or inventing sync.

**Architecture:** Extend the existing workspace date derivation for richer calendar views, and normalize already-approved integration status into a dedicated presentation model. No new provider connection is added in this slice.

**Tech Stack:** ES modules, local workspace store/runtime, existing feature runtime, DOM APIs, Node `node:test`.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- Calendar derives from safe local workspace data only.
- Integration states come only from verified existing adapters.
- No raw third-party credential collection/storage.
- No new OAuth/provider adapter without separate design.
- Current `nova/**` remains untouched.
- TDD first.

---

### Task 1: Rich calendar derivation
**Files:**
- Modify: `nova-next/src/workspace-runtime.mjs`
- Modify: `nova-next/tests/workspace-runtime.test.mjs`

**Interfaces:** add `calendar({ from, to })` returning immutable entries `{ id, kind, title, date, projectId, projectTitle, overdue, completed }`.
- [ ] **Step 1:** Write failing tests for today/upcoming/overdue/completed boundaries, project context and date-range filtering.
- [ ] **Step 2:** Run focused test and confirm RED.
- [ ] **Step 3:** Implement pure derivation from existing tasks/projects; no external fetch.
- [ ] **Step 4:** Run full suite and commit `feat(nova-next): enrich local calendar model`.

### Task 2: Calendar UI
**Files:**
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/calendar-ui-contract.test.mjs`

**Interfaces:** Calendar page groups `Overdue`, `Today`, `Upcoming`, `Completed` and links entries to Task/Project context.
- [ ] **Step 1:** Write failing UI contract for grouping, project labels, empty state and no external-sync claims.
- [ ] **Step 2:** Run and confirm RED.
- [ ] **Step 3:** Implement compact date groups and safe navigation to Tasks/Projects.
- [ ] **Step 4:** Run full tests and commit `feat(nova-next): finish local calendar workspace`.

### Task 3: Integration status presentation
**Files:**
- Create: `nova-next/src/integration-status.mjs`
- Modify: `nova-next/src/feature-runtime.mjs`
- Modify: `nova-next/src/workspace-ui.mjs`
- Create: `nova-next/tests/integration-status.test.mjs`
- Create: `nova-next/tests/integrations-ui-contract.test.mjs`

**Interfaces:** `normaliseIntegration(item)` -> `{ id, label, state, mode, capability, detail }` where state is `connected|disconnected|unavailable|protected|staged` and mode is `read-only|interactive|local-only|protected`.
- [ ] **Step 1:** Write failing tests for Nova session, GitHub status-only, unavailable broker and protected capabilities.
- [ ] **Step 2:** Run and confirm RED.
- [ ] **Step 3:** Implement normalized cards with explicit capability scope and no Connect button for providers that do not have an approved flow.
- [ ] **Step 4:** Run full tests and syntax checks.
- [ ] **Step 5:** Commit `feat(nova-next): clarify integration capability status`.

### Task 4: Verification and PR
- [ ] **Step 1:** Verify no provider credential/OAuth/backend files changed.
- [ ] **Step 2:** Run all Nova Next tests.
- [ ] **Step 3:** Open and auto-merge low/medium-risk PR after green checks and review.
