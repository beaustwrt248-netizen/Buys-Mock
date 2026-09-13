# Nova Next Live Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:test-driven-development for every behavior change and superpowers:verification-before-completion before PR/merge claims.

**Goal:** Connect the isolated Nova Next shell to the same approved Nova authentication and AI service contracts as current Nova, without importing current Nova frontend/runtime code and without granting any new production authority.

**Base:** `nova-next/bootstrap` at `4a8b5834bdaac3024aa40c29d54384ad4d351de9`.

**Risk:** High because this phase touches authentication/session behavior. Implementation and tests may be prepared autonomously, but merge requires explicit human approval for the specific ready PR/head.

## Verified current contracts

- Supabase Auth password exchange uses `/auth/v1/token?grant_type=password` with the existing public publishable key and Turnstile captcha token.
- Session validation uses `/auth/v1/user` plus the existing `profiles` row and must require `role = admin` and `is_enabled = true`.
- Current Nova keeps the access/refresh token only in session-scoped browser storage.
- Nova AI conversation is exposed through authenticated `POST /functions/v1/nova-orchestrator` and remains advisory/guarded.
- The orchestrator accepts `prompt`, optional `mode` (`auto`, `single`, `ensemble`) and optional `provider` (`auto`, `gpt`, `gemini`, `claude`, `consensus`).
- Protected writes remain outside this phase.

## Constraints

- No edits under existing `nova/`, `supabase/`, `android/novaapp/`, `.github/` or production deployment files.
- No service-role/provider/signing secret in Nova Next client source.
- No new Supabase schema/RLS/function changes.
- No direct protected-table access beyond the existing Admin profile validation contract.
- Unknown actions fail closed.
- Authentication failure keeps the application visually/network locked.
- Sign-out clears local session material.

### Task 1: Fresh Supabase REST auth client

**Files:**
- Create: `nova-next/tests/supabase-auth-client.test.mjs`
- Create: `nova-next/src/adapters/supabase-auth-client.mjs`

- [ ] Write RED tests covering password exchange, captcha requirement, user revalidation, profile validation, refresh, timeout/error normalization and sign-out/session clearing behavior.
- [ ] Implement a new fetch-based client from scratch; do not import `morley-auth-client.js` or current Nova code.
- [ ] Verify GREEN.

### Task 2: Session storage boundary

**Files:**
- Create: `nova-next/tests/session-store.test.mjs`
- Create: `nova-next/src/session-store.mjs`

- [ ] Write RED tests for session-only persistence, corrupt-value clearing and no password persistence.
- [ ] Implement minimal store with injected Web Storage object.
- [ ] Verify GREEN.

### Task 3: Edge Function client

**Files:**
- Create: `nova-next/tests/edge-function-client.test.mjs`
- Create: `nova-next/src/adapters/edge-function-client.mjs`

- [ ] Write RED tests for authenticated POST, `apikey`, JSON body, non-2xx normalization and timeout behavior.
- [ ] Implement generic Edge Function invocation over the existing Supabase project URL.
- [ ] Verify GREEN.

### Task 4: Chat / orchestrator adapter

**Files:**
- Create: `nova-next/tests/chat-adapter.test.mjs`
- Create: `nova-next/src/adapters/chat-adapter.mjs`

- [ ] Write RED tests for prompt validation, allowed provider/mode values, guarded response handling, degraded/unavailable responses and auth failures.
- [ ] Implement adapter targeting `nova-orchestrator` only.
- [ ] Verify GREEN.

### Task 5: Turnstile + auth controller

**Files:**
- Create: `nova-next/tests/auth-controller.test.mjs`
- Create: `nova-next/src/auth-controller.mjs`
- Create: `nova-next/src/turnstile.mjs`
- Create: `nova-next/src/runtime-config.mjs`

- [ ] Write RED controller tests using injected fake Turnstile/auth client/session store.
- [ ] Store only public runtime configuration in `runtime-config.mjs`.
- [ ] Implement restore-first boot, captcha-gated sign-in, Admin profile verification, session refresh and fail-closed UI state.
- [ ] Verify GREEN.

### Task 6: Wire Login and Chat UI

**Files:**
- Modify: `nova-next/app.js`
- Modify: `nova-next/index.html` only if necessary
- Modify: `nova-next/styles.css`

- [ ] Keep shell locked until controller reports authenticated Admin.
- [ ] Replace bootstrap preview sign-in with real auth flow.
- [ ] Add safe status/error text and Turnstile host area while preserving the approved reference layout.
- [ ] Connect Chat composer and suggestion buttons to the orchestrator adapter.
- [ ] Render guarded/degraded/unavailable response states without pretending actions executed.
- [ ] Keep all non-chat live capabilities in explicit `not connected yet` parity state rather than fake data.

### Task 7: Security and regression verification

- [ ] Run all Nova Next tests, including bootstrap tests.
- [ ] Run syntax checks on all new modules.
- [ ] Static-scan Nova Next for service-role/provider secrets and imports from `../nova/`.
- [ ] Compare branch to `nova-next/bootstrap`; expected changes only under Nova Next docs/source/tests.
- [ ] Open a **high-risk draft PR** stacked on `nova-next/bootstrap` and leave merge blocked pending explicit conversational approval for that exact PR/head.
