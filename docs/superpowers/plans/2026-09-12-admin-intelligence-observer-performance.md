# Admin Intelligence Observer Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce unnecessary Morley Admin main-thread observer callbacks by narrowing the Intelligence Command Centre readiness observer to the authenticated app container only.

**Architecture:** Keep the existing evidence refresh observers on the specific metrics/lists they consume. Replace only the bootstrap observer that currently watches class mutations across the entire document tree; `appReady()` depends solely on `#appView` being present and not carrying `hidden`, so the bootstrap observer should watch only that element's `class` attribute and disconnect after successful boot.

**Tech Stack:** Vanilla browser JavaScript, Node.js contract tests, GitHub Actions.

**Spec:** `docs/operations/morley-autopilot-state.md`

## Global Constraints

- Preserve Admin authentication, CAPTCHA, role and backend authorization boundaries unchanged.
- Do not modify Guardian, pricing authority, release signing, OTA identity, RLS or production data.
- Keep all evidence-refresh observers scoped to their existing authoritative source elements.
- Do not introduce polling or broad document-level observers.
- Apply TDD: regression must fail on current implementation before production code changes.

---

### Task 1: Define the bounded bootstrap-observer contract

**Files:**
- Modify: `admin/desktop-workspace-stability.contract.test.js`
- Modify: `tests/morley-ecosystem-contract.test.mjs`

**Interfaces:**
- Consumes: `admin/intelligence-command-centre.js` source text.
- Produces: Contract requiring `#appView`-only class observation and rejecting document-wide subtree class observation.

- [ ] **Step 1: Write the failing test**

Add assertions that reject `observe(document.documentElement,{subtree:true,attributes:true,attributeFilter:['class']})` and require a targeted `#appView` observer with `attributes:true` and `attributeFilter:['class']`.

- [ ] **Step 2: Run test to verify it fails**

Run: `node admin/desktop-workspace-stability.contract.test.js && node --test tests/morley-ecosystem-contract.test.mjs`
Expected: FAIL because `intelligence-command-centre.js` still observes class changes across `document.documentElement`.

- [ ] **Step 3: Commit**

Commit the RED regression only.

### Task 2: Narrow the Intelligence Command Centre bootstrap observer

**Files:**
- Modify: `admin/intelligence-command-centre.js`
- Modify: `admin/admin-home.js`

**Interfaces:**
- Consumes: existing `appReady()`, `boot()` and `schedule()` functions.
- Produces: bounded bootstrap observation that watches only `#appView.class` until boot succeeds.

- [ ] **Step 1: Implement the minimal production change**

Replace the document-wide observer with an observer attached to `#appView`, watching only its `class` attribute. Disconnect it once `appReady()` leads to a successful boot. Preserve immediate execution for already-ready Admin sessions.

- [ ] **Step 2: Bump the Intelligence Command Centre cache key**

Advance `intelligence-command-centre.js?v=2` to `v=3` in `admin/admin-home.js` so deployed browsers receive the corrected runtime.

- [ ] **Step 3: Run focused tests**

Run: `node admin/desktop-workspace-stability.contract.test.js && node --test tests/morley-ecosystem-contract.test.mjs`
Expected: PASS.

- [ ] **Step 4: Run repository gates**

Require the existing Quality, Security, Admin integration, UI and Ultimate Parity gates on the exact PR head.

- [ ] **Step 5: Commit**

Commit the minimal runtime/cache update.

### Task 3: Review, merge and verify

**Files:**
- No additional production files unless a gate exposes a real defect.

**Interfaces:**
- Consumes: exact-head CI evidence.
- Produces: merge to `main` only if substantive gates are green and the PR is mergeable.

- [ ] **Step 1: Open the PR and inspect changed files**

Confirm scope is limited to the plan, Admin performance contract, Intelligence Command Centre runtime and cache key.

- [ ] **Step 2: Verify exact-head checks**

Require current Security, Quality, Admin, UI and Ultimate Parity checks to pass. Do not bypass protected workflow failures.

- [ ] **Step 3: Merge the low-risk reversible change**

Merge only when the PR is non-draft, mergeable and substantive required checks are green.

- [ ] **Step 4: Verify post-merge main**

Confirm the merge commit lands on `main` and post-merge automation has no new substantive failures.
