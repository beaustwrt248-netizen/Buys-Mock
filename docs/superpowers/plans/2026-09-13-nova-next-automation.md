# Nova Next Local Automation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Automation useful with truthful local job definitions and statuses, without pretending background execution exists.

**Architecture:** Add an isolated local automation store/runtime plus UI. Jobs are metadata linked to existing Tasks/Projects; there is no timer, background worker, remote scheduler, notification send, or protected execution path in this slice.

**Tech Stack:** ES modules, localStorage, existing workspace runtime, DOM APIs, Node `node:test`.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- Local-only Phase A; no server scheduler or fake background execution.
- No protected actions can be attached to a job.
- Current `nova/**` stays untouched.
- Corrupt/write-failed local state recovers safely.
- TDD first.

---

### Task 1: Local job store
**Files:**
- Create: `nova-next/src/automation-store.mjs`
- Create: `nova-next/tests/automation-store.test.mjs`

**Interfaces:** `createAutomationStore({ storage, now })` -> `list()`, `create(input)`, `update(id, patch)`, `remove(id)`; job `{ id, title, prompt, scheduleText, state, taskId, projectId, lastResult, createdAt, updatedAt }`.
- [ ] **Step 1:** Write failing tests for create/update/delete, immutable results, duplicate IDs, corrupt payload reset, write failure rollback, allowed states `draft|enabled|disabled|completed|failed`.
- [ ] **Step 2:** Run focused test and confirm RED.
- [ ] **Step 3:** Implement isolated key `nova-next.automation.v1` and bound string lengths.
- [ ] **Step 4:** Run focused + full tests.
- [ ] **Step 5:** Commit `feat(nova-next): add local automation store`.

### Task 2: Automation runtime boundary
**Files:**
- Create: `nova-next/src/automation-runtime.mjs`
- Create: `nova-next/tests/automation-runtime.test.mjs`

**Interfaces:** `createAutomationRuntime({ store, workspaceRuntime })` -> `list()`, `save(input)`, `setEnabled(id, enabled)`, `status(job)`.
- [ ] **Step 1:** Write failing tests that reject protected action intent keywords/fields, missing linked task/project IDs, and any claim of background execution.
- [ ] **Step 2:** Run and confirm RED.
- [ ] **Step 3:** Implement status badges `local-only`, `staged`, `protected`; enabling changes metadata only and returns copy `Enabled locally; no background runner is connected.`
- [ ] **Step 4:** Run full tests and commit `feat(nova-next): enforce local automation boundaries`.

### Task 3: Automation UI
**Files:**
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/index.html`
- Modify: `nova-next/live.css`
- Modify: `nova-next/src/feature-runtime.mjs`
- Create: `nova-next/tests/automation-ui-contract.test.mjs`

**Interfaces:** Automation page renders capability status plus local jobs; create/edit controls use `automationRuntime` only.
- [ ] **Step 1:** Write failing UI contract for New Job, linked Task/Project selectors, schedule description, enabled/disabled badge, local-only disclosure and no Run Now/background-success control.
- [ ] **Step 2:** Run and confirm RED.
- [ ] **Step 3:** Implement local job form/list and update existing scheduling status from `staged` to `local-only` while explicitly stating remote execution is not connected.
- [ ] **Step 4:** Run full tests and syntax checks.
- [ ] **Step 5:** Commit `feat(nova-next): add truthful local automation workspace`.

### Task 4: Verification and PR
- [ ] **Step 1:** Verify no Supabase, workflow, notification-send, Guardian, pricing, release or current Nova source changes.
- [ ] **Step 2:** Run complete Nova Next tests.
- [ ] **Step 3:** Auto-merge only if low/medium risk and all checks pass; any persistent scheduler discovery becomes a separate protected design.
