# Nova Next Parity Wave 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the remaining low/medium-risk Nova Next parity gaps with read-only product search, research/evidence UX, voice input, truthful local jobs/alerts, settings polish, and Android parity.

**Architecture:** Keep all changes inside `nova-next/**` plus tests/docs. Reuse the existing authenticated Edge Function client and explicit function allowlist. Add focused adapters/stores for product search, voice input, and local jobs, then wire them into existing feature/workspace UI without adding protected mutation authority.

**Tech Stack:** Vanilla ES modules, DOM APIs, Web Speech API when available, localStorage, Supabase Edge Functions through the existing authenticated client, Node contract tests, Android Gradle wrapper CI.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-parity-wave2-design.md`

## Global Constraints
- Current `nova/**` must not change.
- Product/pricing access is read-only; no pricing approval/write endpoint may be allowlisted.
- Guardian repair, release/OTA, signing, deployment mutation, and user-role mutation stay unavailable.
- Voice input is user-triggered and never auto-sends.
- Local jobs use only `nova-next.jobs.v1` and never claim server/background execution.
- Appearance uses only `nova-next.appearance.v1`.
- All slices require RED-first contracts before implementation.

---

### Task 1: Read-only product and market search

**Files:**
- Create: `nova-next/src/adapters/product-search-adapter.mjs`
- Modify: `nova-next/src/safe-client-functions.mjs`
- Modify: `nova-next/src/safe-services.mjs`
- Modify: `nova-next/src/feature-runtime.mjs`
- Modify: `nova-next/src/feature-ui.mjs`
- Modify: `nova-next/index.html`
- Test: `nova-next/tests/product-search-adapter.test.mjs`
- Test: `nova-next/tests/product-search-ui-contract.test.mjs`

**Interfaces:**
- `createProductSearchAdapter({ edgeClient })`
- `catalogue(query, options?) -> Promise<{devices, prices, device_count}>`
- `market(query, options?) -> Promise<object>`
- `featureRuntime.productCatalogue(query, options?)`
- `featureRuntime.marketSearch(query, options?)`

- [ ] Write failing adapter contract asserting empty-query rejection, invocation of only `app-pricing-catalogue` and `market-search-v2`, local catalogue filtering, and absence of mutation methods.
- [ ] Run the new contract and confirm failure because the adapter/function allowlist does not exist.
- [ ] Add `app-pricing-catalogue` and `market-search-v2` to the explicit safe-client allowlist only after confirming both endpoints revalidate bearer authentication and expose read/search data only.
- [ ] Implement the adapter and service/runtime methods with bounded result normalization.
- [ ] Run adapter tests and confirm pass.
- [ ] Write failing UI contract requiring a real Product & Price Search surface, query form, source labels, read-only boundary copy, and no pricing-write action.
- [ ] Implement the Product Search sheet/page wiring in `feature-ui.mjs` and replace the staged tool behavior.
- [ ] Run product UI and full Nova Next tests.
- [ ] Commit.

### Task 2: Research/evidence UX

**Files:**
- Modify: `nova-next/src/feature-ui.mjs`
- Modify: `nova-next/app.js`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/research-evidence-ui.test.mjs`

**Interfaces:**
- Research continues to use existing guarded `nova-orchestrator` through chat.
- No new backend endpoint.

- [ ] Write failing contract requiring a dedicated research prefill/action, evidence/degraded UI markers, and retry-safe copy.
- [ ] Run and confirm the new test fails on missing markers/behavior.
- [ ] Add research-specific composer metadata and render guarded/degraded/evidence status without changing orchestration authority.
- [ ] Run focused and full tests.
- [ ] Commit.

### Task 3: User-triggered voice input

**Files:**
- Create: `nova-next/src/voice-input.mjs`
- Modify: `nova-next/app.js`
- Modify: `nova-next/index.html`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/voice-input.test.mjs`
- Test: `nova-next/tests/voice-ui-contract.test.mjs`

**Interfaces:**
- `createVoiceInput({ windowObj, onTranscript, onState, onError })`
- `supported() -> boolean`
- `start()`, `stop()`

- [ ] Write failing adapter tests covering unsupported browsers, explicit start, transcript callback, stop/error cleanup, and no form submission API.
- [ ] Implement Web Speech wrapper with `SpeechRecognition || webkitSpeechRecognition` and `continuous=false`, `interimResults=true`.
- [ ] Run adapter test green.
- [ ] Write failing UI contract requiring a microphone button beside the chat composer and copy that transcript is reviewed before sending.
- [ ] Wire transcript into `#novaNextChatInput` only; never submit automatically.
- [ ] Run full tests.
- [ ] Commit.

### Task 4: Truthful local jobs and alerts

**Files:**
- Create: `nova-next/src/local-jobs.mjs`
- Modify: `nova-next/src/feature-ui.mjs`
- Modify: `nova-next/src/feature-runtime.mjs`
- Modify: `nova-next/index.html`
- Test: `nova-next/tests/local-jobs.test.mjs`
- Test: `nova-next/tests/local-jobs-ui-contract.test.mjs`

**Interfaces:**
- `createLocalJobsStore({ storage, key='nova-next.jobs.v1', now, idFactory })`
- CRUD for local definitions only.

- [ ] Write failing store contracts for namespaced persistence, validation, corruption recovery, toggle/delete, and absence of network/timer execution.
- [ ] Implement the local jobs store.
- [ ] Run store tests green.
- [ ] Write failing UI contract requiring local-definition boundary copy and no claim of background execution.
- [ ] Replace staged Scheduling/Automation copy with local job-definition UI while keeping server scheduling marked unconnected.
- [ ] Run full tests.
- [ ] Commit.

### Task 5: Settings/account/appearance/notification truthfulness

**Files:**
- Create: `nova-next/src/local-preferences.mjs`
- Modify: `nova-next/app.js`
- Modify: `nova-next/index.html`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/local-preferences.test.mjs`
- Test: `nova-next/tests/settings-parity.test.mjs`

**Interfaces:**
- appearance key `nova-next.appearance.v1`; values `system|dark|light`.
- notification preference is local informational state only; it must not request push permission or claim delivery.

- [ ] Write failing preference contracts for theme validation/persistence and corrupt-data recovery.
- [ ] Implement local preferences.
- [ ] Write failing settings UI contract requiring real Account status, Appearance selector, Notifications truthfulness, Privacy navigation, and About navigation.
- [ ] Wire Account to authenticated email/status, Appearance to local Nova Next theme classes, Notifications to local preference copy only.
- [ ] Run full tests.
- [ ] Commit.

### Task 6: Integration/calendar parity polish

**Files:**
- Modify: `nova-next/src/feature-ui.mjs`
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/integration-calendar-parity.test.mjs`

**Interfaces:**
- Reuse `integrationStatus()` and workspace-derived calendar.

- [ ] Write failing UI contract requiring refreshable integration status, local-calendar source labeling, empty/error states, and no external credential collection.
- [ ] Implement refresh/retry controls and source labels.
- [ ] Run focused and full tests.
- [ ] Commit.

### Task 7: Android/web parity validation

**Files:**
- Test existing `nova-next/tests/android-wrapper-contract.test.mjs`
- No production package/signing changes.

- [ ] Run all `nova-next/tests/*.test.mjs`.
- [ ] Run JS syntax checks for `nova-next/**/*.js` and `nova-next/**/*.mjs`.
- [ ] Run Android identity contract.
- [ ] Run Android unit tests, lint, and `assembleDebug` through the permanent Nova Next validator workflow.
- [ ] Verify APK hash/artifact generation.
- [ ] Confirm diff contains no `nova/**` or protected backend mutation path.
- [ ] Open/refresh PR and allow low/medium-risk merge only when exact-head checks are green.
