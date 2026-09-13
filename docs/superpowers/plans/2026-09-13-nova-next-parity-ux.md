# Nova Next Parity and UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish Nova Next user-facing parity, state handling, accessibility, settings surfaces and responsive polish without widening authority.

**Architecture:** Keep `app.js` as shell/bootstrap only. Add focused presentation/state helpers under `nova-next/src/`, extend `feature-ui.mjs` and `workspace-ui.mjs` only where they own the affected surfaces, and persist only non-sensitive Nova Next preferences locally.

**Tech Stack:** Vanilla ES modules, DOM APIs, CSS, Node `node:test` contracts, existing Nova Next runtime/store modules.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints

- New runtime code stays under `nova-next/**`.
- Current `nova/**` stays untouched.
- No Guardian repair, pricing write/approval, deployment, release, OTA, signing, role/user mutation, destructive delete, or unrestricted GitHub mutation authority.
- Security-sensitive failures fail closed; non-security failures render truthful unavailable/degraded states.
- UI preferences may use isolated Nova Next local storage; credentials may not.
- TDD: every behavior begins with a failing test.

---

### Task 1: Shared presentation state helpers

**Files:**
- Create: `nova-next/src/presentation-state.mjs`
- Create: `nova-next/tests/presentation-state.test.mjs`

**Interfaces:**
- Produces: `normaliseAsyncState({ status, message, retryable })` and `stateCopy(kind)`.

- [ ] **Step 1: Write the failing test** covering `loading`, `empty`, `unavailable`, `degraded`, `offline`, and `protected` normalization.
- [ ] **Step 2: Run** `node --test nova-next/tests/presentation-state.test.mjs` and confirm failure because the module does not exist.
- [ ] **Step 3: Implement** immutable state objects with exact `kind`, `message`, and `retryable` fields; unknown kinds normalize to `unavailable`.
- [ ] **Step 4: Run** the focused test and then `node --test nova-next/tests/*.test.mjs`.
- [ ] **Step 5: Commit** `feat(nova-next): add truthful presentation states`.

### Task 2: Functional safe Settings surfaces

**Files:**
- Create: `nova-next/src/preferences-store.mjs`
- Create: `nova-next/src/settings-ui.mjs`
- Modify: `nova-next/app.js`
- Modify: `nova-next/index.html`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/settings-ui-contract.test.mjs`
- Create: `nova-next/tests/preferences-store.test.mjs`

**Interfaces:**
- Produces: `createPreferencesStore({ storage })` with `get()`, `setAppearance(value)`, `setNotifications(value)`.
- Produces: `createSettingsUi({ documentObj, preferences, onNavigate, onToast })` with `bind()` and `render()`.

- [ ] **Step 1: Write failing tests** proving appearance accepts only `system|dark|light`, notifications store only local preference state, corrupt storage resets safely, and Account/Privacy/About rows perform truthful actions.
- [ ] **Step 2: Run** the two focused test files and confirm RED.
- [ ] **Step 3: Implement** isolated key `nova-next.preferences.v1`; do not store tokens, email, passwords, or provider credentials.
- [ ] **Step 4: Replace staged Settings click branches** in `app.js` with `settingsUi` wiring; Account shows current Admin session identity read-only, Privacy routes to Files/data boundaries, About routes to Help, Appearance applies a root `data-appearance`, Notifications toggles only Nova Next local preference.
- [ ] **Step 5: Run** focused tests, full Nova Next tests, and `node --check nova-next/app.js nova-next/src/settings-ui.mjs nova-next/src/preferences-store.mjs`.
- [ ] **Step 6: Commit** `feat(nova-next): finish safe settings surfaces`.

### Task 3: Truthful loading/error/retry states

**Files:**
- Modify: `nova-next/src/feature-ui.mjs`
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/feature-state-contract.test.mjs`

**Interfaces:**
- Consumes: `presentation-state.mjs`.

- [ ] **Step 1: Write a failing static/behavior contract** requiring explicit loading, empty, unavailable/degraded copy and safe retry buttons for Knowledge, Control Centre, Integrations and Help.
- [ ] **Step 2: Run** `node --test nova-next/tests/feature-state-contract.test.mjs` and confirm RED.
- [ ] **Step 3: Implement** retry callbacks that repeat only the existing read call; never retry protected actions.
- [ ] **Step 4: Extend workspace empty/offline copy** for Files, Tasks, Projects and Calendar without inventing server sync.
- [ ] **Step 5: Run** focused and full suites.
- [ ] **Step 6: Commit** `feat(nova-next): add resilient feature states`.

### Task 4: Accessibility and responsive polish

**Files:**
- Modify: `nova-next/app.js`
- Modify: `nova-next/index.html`
- Modify: `nova-next/styles.css`
- Modify: `nova-next/live.css`
- Modify: `nova-next/tests/accessibility-contract.test.mjs`
- Create: `nova-next/tests/responsive-contract.test.mjs`

**Interfaces:**
- No new network/runtime interfaces.

- [ ] **Step 1: Extend failing contracts** for 44px minimum primary touch targets, `aria-live` status regions, dialog/sheet semantics, visible focus, reduced-motion override, and keyboard-safe composer spacing using `100dvh`/safe-area variables.
- [ ] **Step 2: Run** accessibility/responsive tests and confirm RED.
- [ ] **Step 3: Implement** CSS/ARIA/focus fixes while preserving the approved deep-navy blue/purple hierarchy.
- [ ] **Step 4: Run** all Nova Next tests and JS syntax checks.
- [ ] **Step 5: Commit** `fix(nova-next): polish accessibility and mobile layout`.

### Task 5: Slice verification and PR

**Files:** no new production files.

- [ ] **Step 1: Run** `node --test nova-next/tests/*.test.mjs`.
- [ ] **Step 2: Run** `node --check nova-next/app.js nova-next/src/*.mjs nova-next/src/adapters/*.mjs`.
- [ ] **Step 3: Confirm diff contains no `nova/**`, `supabase/**`, `.github/workflows/**`, Android signing/package promotion, Guardian, pricing-write or release files.
- [ ] **Step 4: Open a low/medium-risk PR**, wait for required checks, review diff/threads, and merge automatically only when green.
