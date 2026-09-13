# Morley Login, Session, and Loading Reliability Design

## Purpose

This is the first implementation slice of the approved Morley ecosystem hardening sweep. The larger sequence is:

1. Login/session/loading reliability.
2. Morley Admin web/mobile parity.
3. Camera/scanner workflow reliability.
4. Catalogue/inventory consistency.
5. Responsive web/mobile cleanup.
6. Guardian/runtime error cleanup.
7. Full regression, deployment, and live verification.

Each slice is intentionally separate so failures can be isolated, reviewed, reverted, and verified without combining unrelated production risk.

## Goal

Make sign-in, temporary-password flows, session restoration, logout, account switching, and initial app loading reliable across Morley Buys and Morley Admin without replacing the existing authentication backend or changing protected pricing/business logic.

The user should never be trapped on a spinner, silently lose a valid session, get stuck after a temporary-password challenge, or see one surface behave as signed in while another behaves as signed out.

## Scope

This slice covers:

- Morley Buys web authentication entry and session restore.
- Morley Admin web authentication entry and session restore.
- Shared authentication bootstrap/runtime helpers used by those surfaces.
- Temporary-password / forced-password-change transitions already present in the repository.
- Initial loading and authenticated-route handoff.
- Logout and stale-session cleanup.
- Error, offline, timeout, and retry behavior around authentication bootstrap.
- Regression contracts for web/mobile-browser ownership boundaries.

This slice does not redesign authentication UI, replace Supabase/auth providers, change user roles/permissions policy, modify pricing algorithms, rebuild mobile-native authentication, or weaken Guardian/protected-repair boundaries.

## Current Reliability Risks to Eliminate

The hardening pass treats these as the expected failure classes to audit and remove:

- Multiple auth bootstrap paths racing to own startup.
- Duplicate listeners or repeated session subscriptions after reload/resize/re-entry.
- Indefinite loading state when auth initialization rejects, times out, or returns an unexpected challenge.
- Temporary-password challenge state being interpreted as a normal authenticated session too early.
- Authenticated content rendering before role/permission state is ready.
- Valid sessions being discarded because local/runtime state and provider state disagree briefly during startup.
- Stale local auth markers surviving a real provider logout.
- Logout only clearing one surface while another shared runtime still appears authenticated.
- Error recovery requiring a full browser restart instead of a controlled retry.
- Mobile-browser layout/runtime paths accidentally invoking desktop-only bootstrap behavior.

## Design Principles

1. **One startup owner.** A single auth bootstrap coordinator owns startup state for each page. Existing auth APIs remain the source of truth.
2. **Provider state beats local hints.** Local storage/session markers may speed rendering but never override provider-confirmed authentication state.
3. **Finite loading states.** Every loading state must terminate into authenticated, challenge-required, signed-out, recoverable-error, or offline states.
4. **Challenge-aware transitions.** Temporary-password / password-update requirements are first-class states, not generic login failures.
5. **Idempotent subscriptions.** Auth/session listeners install once and can be safely torn down or re-entered without duplication.
6. **Permission handoff is explicit.** Authenticated UI opens only after the minimum role/permission context required by that surface is available.
7. **No hidden fallbacks.** Missing auth capability or failed bootstrap must show a controlled state rather than silently routing to an unrelated page.
8. **Mobile remains authoritative on mobile.** Desktop hardening must not take ownership of mobile-browser/native authentication presentation.
9. **No backend migration in this slice.** Reliability is improved around the existing backend and contracts.

## Architecture

### Auth bootstrap coordinator

Introduce or consolidate a small coordinator around the existing authentication runtime. It should expose a finite state model similar to:

- `booting`
- `signed_out`
- `challenge_required`
- `authenticated_pending_context`
- `authenticated_ready`
- `offline_recoverable`
- `error_recoverable`

The coordinator must not duplicate credential validation. It consumes the current provider/session functions and translates their outcomes into deterministic UI/runtime states.

### Session source of truth

Startup sequence:

1. Initialize the existing provider/runtime once.
2. Read the current provider session.
3. If no provider session exists, clear stale local auth markers and show signed-out state.
4. If the provider reports a required password/challenge transition, enter `challenge_required` and block normal app routing.
5. If a valid session exists, resolve required role/permission/profile context.
6. Enter `authenticated_ready` only after that context is sufficient for the current surface.
7. Subscribe once to future auth-state changes.

### Timeout and retry behavior

Auth bootstrap must have a bounded timeout. A timeout does not imply signed-out; it enters a recoverable error/offline state and offers a retry that reuses the same coordinator rather than reloading duplicate runtimes.

Retry must be idempotent and must not create another auth-state subscription.

### Temporary-password flow

The temporary-password flow must preserve the provider's challenge state across the password-update screen. Success transitions through a fresh provider/session read before authenticated content opens. Failure leaves the user on the challenge screen with the actual recoverable error and does not partially initialize the app behind it.

### Logout

Logout order:

1. Call the existing provider sign-out path.
2. Wait for provider state confirmation or bounded failure handling.
3. Clear only Morley-owned local/session auth hints and sensitive cached identity context.
4. Return the surface to deterministic signed-out state.
5. Avoid full-page loops or repeated logout handlers.

If provider logout fails because the network is unavailable, the UI should clearly show the failure and avoid claiming the remote session was revoked.

## UI and Loading Behavior

- A loading spinner must be paired with an actual coordinator state and bounded timeout.
- Loading copy should distinguish "Signing you in", "Restoring your session", and "Loading your workspace" where the existing UI supports it.
- Temporary-password challenges must not flash the normal dashboard first.
- Recoverable errors should provide retry; unrecoverable configuration errors should provide diagnostics/support context instead of infinite retries.
- Existing visual design should be preserved in this slice unless a small UI adjustment is necessary to make state truthful.

## Data and State Boundaries

Allowed to reuse:

- Existing auth provider/session APIs.
- Existing user/profile/role/permission loaders.
- Existing local/session storage keys that are still required.
- Existing login and temporary-password forms.
- Existing diagnostics/event telemetry where present.

Not allowed:

- New credential stores.
- Persisting passwords or temporary passwords.
- New auth backend schemas unless a discovered repository bug proves one is strictly required and the design is upgraded/re-approved.
- Treating local storage alone as an authenticated session.

## Error Handling

Every auth operation should classify failure into one of these groups:

- Invalid credentials / challenge input.
- Session expired or revoked.
- Network/offline/timeout.
- Permission/profile-context load failure.
- Configuration/runtime initialization failure.

Errors should be surfaced through existing error UI when possible. Logging must avoid credentials, tokens, and sensitive session payloads.

## Testing Strategy

Add focused regression coverage before changing implementation. The contract suite should require:

- A single auth bootstrap owner per surface.
- Finite startup states; no unbounded spinner-only path.
- Temporary-password challenge blocks normal authenticated routing until completion.
- Session restore reads provider state before trusting local markers.
- Logout clears local Morley auth hints only after attempting provider sign-out.
- Retry does not duplicate subscriptions/listeners.
- Permission/profile context is resolved before authenticated-ready state.
- Offline/timeout produces recoverable state instead of forced logout.
- Mobile/physical-phone ownership remains isolated from desktop-specific logic.

Where existing end-to-end or smoke workflows cover login, extend them rather than creating a parallel test stack.

## Release and Verification

Implementation will use an isolated branch based on the current `main` and will not merge directly to production.

Before merge, require the repository's applicable security, auth, feature-contract, UI, quality, parity, release-smoke, and restore-point gates to pass on the exact head SHA.

After merge, verify the exact merged SHA through the production deployment workflow, post-deploy smoke tests, and any live auth/web contract workflows available in the repository before declaring this slice complete.

## Success Criteria

This slice is complete when:

- Normal sign-in reaches the correct authenticated workspace without duplicate initialization.
- Existing sessions restore reliably after refresh/reopen.
- Temporary-password users complete the challenge without loops or dashboard flashes.
- Invalid/expired sessions resolve cleanly to signed-out state.
- Offline/slow-provider startup does not falsely sign the user out or freeze indefinitely.
- Logout leaves Morley Buys/Admin in a consistent signed-out state.
- Re-entry/retry does not accumulate duplicate auth listeners.
- Existing permissions and auth-provider contracts remain intact.
- Required CI/release/live verification passes on the merged production SHA.

## Deferred Slices

After this slice is merged and live-verified, the next independent spec will cover Morley Admin web/mobile functionality parity. Camera/scanner, catalogue/inventory, responsive cleanup, and Guardian/runtime cleanup remain separate follow-on specs so they do not obscure authentication regressions or expand rollback scope.
