# Nova Next Price and Product Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the staged Price & Product Search tool with a real read-only search experience, but stop before backend work if no approved safe contract already exists.

**Architecture:** First audit existing safe Nova functions/services for a read-only product-search path. If one exists, add a narrow adapter/runtime/UI module. If none exists, ship only a truthful dependency surface and open a separate protected backend design; do not repurpose pricing-write or privileged endpoints.

**Tech Stack:** ES modules, existing edge-function client and safe-service registry, Node `node:test`, DOM UI.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- Read/search/recommendation only; no catalogue mutation, pricing approval/write, checkout, seller contact or financial action.
- Current `nova/**` remains untouched by the client slice.
- No new backend function is allowlisted without a separately approved design.
- Stale/unknown prices are explicitly labelled.
- TDD for normalization, authority boundaries and UI states.

---

### Task 1: Audit safe search authority
**Files:** `nova-next/src/safe-client-functions.mjs`, `nova-next/src/safe-services.mjs`, `nova-next/src/adapters/*.mjs` (read only for audit); create `nova-next/tests/product-search-boundary.test.mjs` only if a safe path exists.
- [ ] **Step 1:** Search repository contracts for product/catalogue/pricing read-only functions and verify exact operation names and response shapes.
- [ ] **Step 2:** Classify result: `SAFE_EXISTING_CONTRACT` or `BACKEND_DESIGN_REQUIRED` in the PR notes.
- [ ] **Step 3:** If `BACKEND_DESIGN_REQUIRED`, do not add an allowlist entry; proceed only with a truthful UI explaining the dependency, then stop backend work for protected approval.

### Task 2: Read-only adapter and normalization (only when safe contract exists)
**Files:**
- Create: `nova-next/src/adapters/product-search-adapter.mjs`
- Modify: `nova-next/src/safe-services.mjs`
- Modify: `nova-next/src/feature-runtime.mjs`
- Create: `nova-next/tests/product-search-adapter.test.mjs`

**Interfaces:** `createProductSearchAdapter({ edgeClient })` -> `search({ query, condition, storage, carrier, region })`; normalized item `{ id, title, sourceLabel, sourceUrl, observedPrice, currency, condition, variant, confidence, observedAt, freshness }`.
- [ ] **Step 1:** Write failing tests for input validation, normalization, missing price/freshness, immutable output and rejection of mutation-like fields/actions.
- [ ] **Step 2:** Run focused test and confirm RED.
- [ ] **Step 3:** Implement only the audited read operation and normalized result mapping.
- [ ] **Step 4:** Run focused + full tests and syntax checks.
- [ ] **Step 5:** Commit `feat(nova-next): add guarded product search adapter`.

### Task 3: Product search UI
**Files:**
- Create: `nova-next/src/product-search-ui.mjs`
- Modify: `nova-next/index.html`
- Modify: `nova-next/src/feature-ui.mjs`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/product-search-ui-contract.test.mjs`

**Interfaces:** `createProductSearchUi({ documentObj, featureRuntime, onNavigate, onToast })` -> `bind()`, `open()`, `search()`.
- [ ] **Step 1:** Write failing contract requiring query + optional filters, loading/empty/error states, source/freshness labels, no mutation controls, and “Compare in Chat” handoff.
- [ ] **Step 2:** Run focused test and confirm RED.
- [ ] **Step 3:** Implement the dedicated search sheet/page; compare handoff inserts a descriptive prompt into Chat and requires the user to send it.
- [ ] **Step 4:** If backend dependency remains, render `Read-only search backend not connected` with no fake results.
- [ ] **Step 5:** Run full suite and commit `feat(nova-next): finish price and product search surface`.

### Task 4: Verify authority boundary and PR
- [ ] **Step 1:** Run `node --test nova-next/tests/*.test.mjs` and JS syntax checks.
- [ ] **Step 2:** Prove diff contains no pricing writes/approvals, catalogue mutation, checkout/seller actions, new protected allowlist entries, or current Nova source edits.
- [ ] **Step 3:** Merge automatically only if this remains low/medium risk and all checks pass; otherwise stop at exact protected-head approval.
