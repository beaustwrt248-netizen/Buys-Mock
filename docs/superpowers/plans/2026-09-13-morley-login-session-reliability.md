# Morley Login, Session, and Loading Reliability Implementation Plan

> **Execution:** Use Superpowers `executing-plans`, `test-driven-development`, `systematic-debugging` on failures, and `verification-before-completion` before any completion claim.

**Goal:** Make Morley Buys and Morley Admin sign-in, session restore, temporary-password/challenge handling, loading, retry, and logout deterministic without replacing the existing auth provider or weakening permission/Guardian boundaries.

**Design:** `docs/superpowers/specs/2026-09-13-morley-login-session-reliability-design.md`

## Existing auth ownership discovered

- `morley-auth-client.js` already provides the policy-free REST auth primitive with a 12s request timeout, profile validation, refresh-token grant, and captcha-aware password exchange.
- `web-auth.js` currently owns Morley Buys local session persistence and UI, but it directly treats the local `morley_web_auth` object as the primary startup source and clears it on most refresh/profile failures.
- `secure-current-signout.js` currently swallows provider logout failures, clears local auth state anyway, and reloads.
- `admin/browser-auth-bootstrap.js` owns the logged-out Admin browser bootstrap and independently creates a Supabase client.
- `admin/login-security.js` owns Admin Turnstile/password sign-in and calls `loadSession()` after sign-in.
- `admin/workspace.html` independently creates another Supabase client and rechecks the provider session/profile before loading privileged scripts.
- `admin/app.js` creates yet another client, defines another `loadSession()`, owns workspace logout, and immediately calls `loadSession()` again after the workspace shell has already authorised the user.
- Existing auth coverage includes `tests/morley-auth-client-contract.test.mjs`, `tests/admin-auth-recovery-contract.test.mjs`, `tests/admin-browser-turnstile-contract.test.mjs`, and `tests/admin-auth-boundary-parity.test.mjs`.

The implementation will consolidate *startup ownership* and state translation while preserving provider APIs and current security boundaries.

---

## Task 1 — Add failing reliability contracts

**Files**
- Add: `tests/morley-auth-session-reliability.test.mjs`
- Extend: `tests/morley-auth-client-contract.test.mjs`
- Extend only where necessary: `tests/admin-auth-recovery-contract.test.mjs`

**Red-phase requirements**

Add source-level and executable contracts requiring:

1. A finite shared state vocabulary: `booting`, `signed_out`, `challenge_required`, `authenticated_pending_context`, `authenticated_ready`, `offline_recoverable`, `error_recoverable`.
2. A bounded bootstrap helper that distinguishes timeout/network failure from a confirmed signed-out provider result.
3. Morley Buys session restore must validate/refresh against provider state before accepting a local session hint as authenticated.
4. Refresh/profile timeout must not immediately erase a potentially valid local session.
5. Logout must attempt provider revocation and surface failure before clearing local state.
6. Admin login bootstrap must be single-flight and expose deterministic status instead of multiple independent `loadSession()` owners racing.
7. Admin workspace must mark provider/profile context as authorised before privileged runtime is loaded; `admin/app.js` must reuse that context instead of immediately creating another startup race.
8. Retry paths must be idempotent.
9. Existing Turnstile same-origin isolation and Android/native ownership contracts remain unchanged.

**Run**

`node --test tests/morley-auth-client-contract.test.mjs tests/morley-auth-session-reliability.test.mjs tests/admin-auth-recovery-contract.test.mjs tests/admin-browser-turnstile-contract.test.mjs tests/admin-auth-boundary-parity.test.mjs`

Expected first result: FAIL on the new reliability assertions.

Commit after red tests: `test: define Morley auth session reliability contract`

---

## Task 2 — Harden shared auth primitives without adding policy

**Files**
- Modify: `morley-auth-client.js`
- Modify: `tests/morley-auth-client-contract.test.mjs`

**Implementation**

- Keep `MorleyAuthClient` policy-free.
- Expose explicit error classification from the request layer without tokens or credential payloads: timeout, network/offline, HTTP/auth, configuration.
- Preserve the existing 12s request bound.
- Stop `refreshSession()` from collapsing all failures into `null`; return `null` only for a confirmed non-refreshable/no-token case, while network/timeout/provider errors remain distinguishable to callers.
- Preserve explicit role policy in `validateProfile()`.
- Do not add Admin-specific roles, storage keys, UI state, or Supabase schema logic.

**Tests**

Add executable cases for timeout and failed refresh classification using mocked fetch/AbortController behavior. Confirm no privileged secrets/policies enter the shared client.

Commit: `fix: preserve auth failure semantics in shared client`

---

## Task 3 — Make Morley Buys restore finite and provider-confirmed

**Files**
- Modify: `web-auth.js`
- Modify: `index.html` only to bump cache keys/load order if required
- Add/modify: `tests/morley-auth-session-reliability.test.mjs`

**Implementation**

- Add one module-local bootstrap owner (`bootstrapPromise`/state guard) instead of allowing repeated startup work.
- Introduce the finite state constants from the design and a single `setAuthState()` path for status/UI transitions.
- Treat local `morley_web_auth` as a restoration hint, not proof of authentication.
- For an unexpired access token, provider/profile validation must complete before entering `authenticated_ready`.
- For refreshable sessions, refresh first; only a confirmed expired/revoked/unauthorised result clears the local session.
- Timeout/offline/network failure enters `offline_recoverable` or `error_recoverable` and keeps the recoverable local hint intact.
- Add a bounded retry action that reuses the same bootstrap owner after reset and cannot create duplicate listeners/timers.
- Preserve existing visual login design, Turnstile flow, invite signup, password reset, carousel, mobile behavior, and desktop boundary.
- Do not claim support for a provider password challenge that the current REST flow does not expose. Instead, introduce an explicit `challenge_required` hook/state that can be entered only when a real provider response signals a password/recovery challenge; never infer it from a generic error.

**Tests**

Require provider verification before ready, local hint retention on timeout, clearing only on confirmed invalid/revoked/disabled state, finite retry state, and no duplicate bootstrap owner.

Commit: `fix: make Morley web session restore deterministic`

---

## Task 4 — Make Morley Buys logout truthful

**Files**
- Modify: `secure-current-signout.js`
- Modify: `tests/morley-auth-session-reliability.test.mjs`

**Implementation**

- Keep the single capture-phase signout owner.
- Bound the provider logout call.
- On successful provider logout (or confirmed already-expired/invalid session), clear `morley_web_auth` and sensitive Morley-owned identity hints, then return to signed-out state/reload once.
- On network/timeout failure, do not claim remote revocation and do not silently erase the only recoverable session state. Surface a recoverable message/event that existing UI can display and allow retry.
- Preserve `handling` single-flight protection and add a finally/reset path when retry is appropriate.

**Tests**

Require provider attempt before local clearing, failure preservation, one reload on success, and no token logging.

Commit: `fix: make Morley signout provider-confirmed`

---

## Task 5 — Consolidate Admin browser bootstrap ownership

**Files**
- Modify: `admin/browser-auth-bootstrap.js`
- Modify: `admin/login-security.js`
- Modify: `admin/index.html` for cache keys only
- Extend: `tests/admin-auth-recovery-contract.test.mjs`
- Extend: `tests/admin-browser-turnstile-contract.test.mjs`

**Implementation**

- Keep the logged-out Admin document auth-only.
- Make `browser-auth-bootstrap.js` the sole provider-session/profile bootstrap owner on the login page.
- Add single-flight `loadSession()` behavior and finite status outcomes.
- A provider/session error or profile-network timeout must stay on login with a recoverable status and retry; absence of a session is the only normal signed-out state.
- Keep role policy exactly `admin`/`manager` and enabled-profile enforcement.
- `login-security.js` continues to own Turnstile and password submission, but after successful `signInWithPassword()` it calls the same single-flight bootstrap rather than creating a second authorisation path.
- Preserve native `nativeAuth=1` ownership and the existing same-origin Turnstile contracts.

**Tests**

Verify one bootstrap owner, single-flight guard, profile check before redirect, no privileged scripts on login page, Turnstile security unchanged, and cache-key updates.

Commit: `fix: serialize Admin browser authentication bootstrap`

---

## Task 6 — Remove the Admin workspace double-bootstrap race

**Files**
- Modify: `admin/workspace.html`
- Modify: `admin/app.js`
- Modify: `admin/auth-boundary.js` only if a deterministic ready marker is required
- Extend: `tests/admin-auth-recovery-contract.test.mjs`
- Extend: `tests/admin-auth-boundary-parity.test.mjs`

**Implementation**

- Keep `workspace.html` as the security gate: provider `getSession()` → enabled admin/manager profile → template → privileged scripts.
- After successful provider/profile validation, publish a minimal non-secret authorised context on `window` (user id/profile object already returned by provider), and set a deterministic workspace bootstrap state before loading `app.js`.
- Change `admin/app.js` to consume that already-authorised context instead of immediately calling another independent `sb.auth.getSession()` / profile query during startup.
- `admin/app.js` may still revalidate on explicit auth-change/retry events, but startup must have one owner.
- Keep `auth-boundary.js` DOM protection and authenticated-class behavior; make any observer/listener installation idempotent if touched.
- Workspace load/template/script failures must show a bounded recoverable error instead of a rapid redirect/reload loop.
- Preserve Android native Admin isolation completely.

**Tests**

Require workspace authorisation before script loading, `app.js` context reuse, no immediate duplicate startup query, fail-closed boundary, and no Android changes.

Commit: `fix: remove Admin workspace auth startup race`

---

## Task 7 — Make Admin logout and re-entry deterministic

**Files**
- Modify: `admin/app.js`
- Extend: `tests/morley-auth-session-reliability.test.mjs` or `tests/admin-auth-recovery-contract.test.mjs`

**Implementation**

- Guard Admin logout as single-flight.
- Await `sb.auth.signOut()` result.
- On success clear only Admin/Morley-owned runtime identity context and navigate once to the login document.
- On provider/network failure keep the workspace state truthful and show a recoverable error; do not claim logout succeeded.
- Ensure pageshow/visibility/retry cannot install duplicate auth handlers.

Commit: `fix: make Admin logout and re-entry deterministic`

---

## Task 8 — Integration, mobile boundary, and cache contract

**Files**
- Modify cache/version references only where changed assets require it: `index.html`, `admin/index.html`, `admin/workspace.html`
- Update focused contract tests

**Verification**

Run the focused Node suite from Task 1, then all repository commands used by the relevant auth/security/UI workflows. At minimum inspect and execute the exact commands from:

- Repository Security Audit
- Full Feature Contract Audit
- B&L Morley Quality Gate
- Morley Ultimate Parity Gate
- Web Release Smoke Checks
- Morley UI Consistency / UI Checklist when UI/cache files trigger them
- Morley Restore Point Capture
- Admin auth recovery/browser Turnstile contracts

Confirm `html.morley-physical-phone`, Android Admin native session ownership, pricing/business logic, Supabase schemas, and Guardian protected repair boundaries are unchanged.

Commit: `test: verify Morley auth reliability integration`

---

## Task 9 — PR, protected merge, and production verification

- Compare branch with current `main`; reconcile any new main commits without overwriting them.
- Open PR: `Harden Morley login, session and loading reliability`.
- Complete repository UI/mobile checklist evidence truthfully; mark unchanged native/mobile surfaces as boundary-reviewed rather than claiming redesign.
- Require all applicable exact-head gates green. Never bypass a failing gate.
- Request/review comments and fix valid findings test-first.
- Merge only through repository protections after exact-head verification.
- Verify merged SHA equals `main`.
- Require successful `Deploy B&L Morley Web`, post-deploy smoke tests, and available live web/auth contracts on the merged SHA.
- Only then mark this slice complete and begin the separate Admin web/mobile parity spec.
