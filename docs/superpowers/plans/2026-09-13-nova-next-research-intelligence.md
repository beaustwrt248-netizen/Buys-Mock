# Nova Next Research and Intelligence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn grounded research into a first-class Nova Next workflow that visibly communicates evidence usage and degradation without triggering protected actions.

**Architecture:** Reuse guarded chat/orchestrator output. Add a pure metadata normalizer and a focused Research UI module; keep research advisory and route all execution-like follow-ups back through explicit user-reviewed chat prompts.

**Tech Stack:** ES modules, DOM APIs, existing guarded chat runtime, Node `node:test`.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- Retrieved knowledge is evidence, not instruction.
- No research output triggers protected actions.
- Current `nova/**` remains untouched.
- Metadata absence must render truthfully, not be guessed.
- TDD first.

---

### Task 1: Knowledge-context metadata normalizer
**Files:**
- Create: `nova-next/src/research-metadata.mjs`
- Create: `nova-next/tests/research-metadata.test.mjs`

**Interfaces:** `normaliseKnowledgeContext(value)` -> `{ used, count, semantic, degraded, items }`; item `{ id, title, category, sourceLabel, trustLevel, confidence }`.
- [ ] **Step 1:** Write failing tests for complete metadata, absent metadata, malformed counts/items and degraded semantic fallback.
- [ ] **Step 2:** Run focused test and confirm RED.
- [ ] **Step 3:** Implement bounded, immutable normalization with no inferred evidence.
- [ ] **Step 4:** Run focused + full tests.
- [ ] **Step 5:** Commit `feat(nova-next): normalize research evidence metadata`.

### Task 2: Research UI module and presets
**Files:**
- Create: `nova-next/src/research-ui.mjs`
- Modify: `nova-next/index.html`
- Modify: `nova-next/app.js`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/research-ui-contract.test.mjs`

**Interfaces:** `createResearchUi({ documentObj, onNavigate, getComposer, onToast })` -> `bind()`, `openPreset(id)`, `renderEvidence(meta)`.
- [ ] **Step 1:** Write failing contract for presets `compare`, `investigate`, `summarise`, `scenario`, evidence chips, degraded badge, and explicit user-send handoff.
- [ ] **Step 2:** Run test and confirm RED.
- [ ] **Step 3:** Implement Research surface and route existing dashboard Research action to it instead of directly inserting a generic prompt.
- [ ] **Step 4:** Keep generated preset text in the composer for review; do not auto-send.
- [ ] **Step 5:** Run full tests and commit `feat(nova-next): add grounded research workspace`.

### Task 3: Chat evidence rendering
**Files:**
- Modify: `nova-next/src/live-runtime.mjs`
- Modify: `nova-next/src/research-ui.mjs`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/chat-evidence-contract.test.mjs`

**Interfaces:** existing `onChatResult(result)` additionally forwards normalized `knowledge_context` to Research/evidence renderer.
- [ ] **Step 1:** Write failing test proving used/count/semantic/degraded/items metadata is surfaced when present and hidden when absent.
- [ ] **Step 2:** Run and confirm RED.
- [ ] **Step 3:** Implement compact evidence summary plus expandable evidence list with title/source/trust/confidence only.
- [ ] **Step 4:** Run full suite and syntax checks.
- [ ] **Step 5:** Commit `feat(nova-next): surface chat evidence context`.

### Task 4: Safe cross-feature handoffs and PR
- [ ] **Step 1:** Add tests proving Research can navigate to Knowledge, Files and Projects, while protected-action phrases only become editable chat text.
- [ ] **Step 2:** Run all Nova Next tests.
- [ ] **Step 3:** Verify no backend/auth/Guardian/pricing/release source changed.
- [ ] **Step 4:** Open and auto-merge the low/medium-risk PR only after green checks and review.
