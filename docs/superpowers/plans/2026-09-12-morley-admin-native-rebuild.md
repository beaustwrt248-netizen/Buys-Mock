# Morley Admin Native Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the authenticated Morley Admin WebView workspace with a fully native Compose workspace that owns one Android session and cannot fall back to the website login.

**Architecture:** Keep `AdminLoginActivity` as the native credential + Turnstile boundary, store the authorized `AdminSession` in a process-scoped native session owner, and make `AdminActivity` a Compose-only authenticated shell backed directly by `AdminApi`. Reuse the existing native operational panels and Supabase REST/RLS boundaries; the only WebView remaining in the normal app is the scoped Turnstile challenge.

**Tech Stack:** Kotlin, Android Compose Material 3, coroutines, `HttpURLConnection`, Supabase Auth/REST, JUnit 4, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-12-morley-admin-native-rebuild-design.md`

## Global Constraints

- Android application ID remains `com.buysloans.admin`.
- `AdminActivity` must contain no `WebView`, `evaluateJavascript`, `loadUrl`, `installNativeAdminSession`, `/admin/native-logout`, or JavaScript bridge.
- Exactly one Android JavaScript bridge remains, scoped to Turnstile login.
- Access/refresh tokens must not be passed through Android intents or any web content.
- Supabase/RLS and existing role policies remain authoritative.
- Session persistence is process-scoped for this recovery release; no disk persistence is added.
- OTA/update failure remains non-blocking unless metadata marks an update required.
- Release version advances from 0.1.40 (41) to 0.1.41 (42).

---

### Task 1: Add the native session owner and regression tests

**Files:**
- Create: `android/adminapp/src/main/java/com/buysloans/admin/AdminSessionStore.kt`
- Create: `android/adminapp/src/test/java/com/buysloans/admin/AdminSessionStoreTest.kt`

**Interfaces:**
- Produces: `AdminSessionStore.current(): AdminSession?`, `AdminSessionStore.set(session: AdminSession)`, `AdminSessionStore.clear()`, `AdminSessionStore.hasAuthorizedSession(): Boolean`.
- Consumes: existing `AdminSession` and `AdminAppAccessPolicy`.

- [ ] Write unit tests proving an empty store denies entry, an authorized session can be read, and `clear()` removes the session.
- [ ] Run `gradle :adminapp:testDebugUnitTest --tests com.buysloans.admin.AdminSessionStoreTest` and verify RED before implementation.
- [ ] Implement a synchronized process-scoped store that does not persist tokens and validates the session before reporting it authorized.
- [ ] Run the focused unit test and verify GREEN.

### Task 2: Make native login install the authorized Android session before navigation

**Files:**
- Modify: `android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt`
- Create: `android/adminapp/src/test/java/com/buysloans/admin/AdminNativeLoginNavigationContractTest.kt`

**Interfaces:**
- Consumes: `AdminSessionStore.set(session)`.
- Produces: navigation to `AdminActivity` with no token extras.

- [ ] Write a source contract test that fails while `EXTRA_ACCESS_TOKEN` / `EXTRA_REFRESH_TOKEN` intent handoff remains.
- [ ] Verify the test fails.
- [ ] Change successful login to `AdminSessionStore.set(session)` before starting `AdminActivity`; use task/back-stack flags that prevent returning to the signed-in login activity.
- [ ] Ensure failed sign-in does not populate the store and still resets Turnstile.
- [ ] Run login/session tests and verify GREEN.

### Task 3: Replace `AdminActivity` with a Compose-only authenticated shell

**Files:**
- Replace: `android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt`
- Create: `android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt`
- Create: `android/adminapp/src/test/java/com/buysloans/admin/AdminNativeWorkspaceContractTest.kt`

**Interfaces:**
- Consumes: `AdminSessionStore.current()`, `AdminApi.load(session)`, `SupportOperationsPanel`, `GuardianPanel`, `ManualNotificationPanel`, `UserManagementPanel`, `AuditTimelinePanel`, existing user-control/remote-config helpers.
- Produces: native Overview, Support, Guardian, Notifications, Users/devices, Audit, Controls and Release navigation.

- [ ] Write contract tests asserting `AdminActivity` contains no WebView/session-injection/browser-route symbols and does require a native stored session.
- [ ] Verify the new contract is RED against the current WebView activity.
- [ ] Implement `AdminActivity` as a Material 3 Compose host. If no valid stored session exists, route to `AdminLoginActivity` and finish.
- [ ] Implement `AdminNativeDashboard` with a mobile-safe vertical layout, current role identity, Refresh and Sign out actions, and workspace navigation.
- [ ] On first composition load `AdminApi.load(session)`; show recoverable native error with Refresh + Sign out instead of redirecting to web.
- [ ] Wire Support, Guardian, Notifications, Users/devices and Audit to existing native components.
- [ ] Implement native overview metrics and read-only production health from `AdminSnapshot`.
- [ ] Ensure `staff` sees the support-only workspace and cannot navigate into full-snapshot-only panels.
- [ ] Run unit tests and debug assembly/lint.

### Task 4: Native sign-out and expired-session fail-closed behavior

**Files:**
- Modify: `android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt`
- Modify: `android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt`
- Modify: `android/adminapp/src/main/java/com/buysloans/admin/AdminApi.kt` only if needed to expose a stable session-expired classification without weakening auth checks.
- Extend: `AdminSessionStoreTest.kt`, `AdminNativeWorkspaceContractTest.kt`

**Interfaces:**
- Produces: `signOut()` behavior that clears the store and clears the authenticated task stack.

- [ ] Add tests proving sign-out clears the store and no browser logout route exists.
- [ ] Add a session-expiry path: if refresh ultimately fails with the existing `Admin session expired. Sign in again.` condition, clear the store and return to native login.
- [ ] Keep ordinary network/snapshot failures recoverable in place.
- [ ] Run focused and full Admin unit tests.

### Task 5: Remove obsolete Android WebView-auth contracts and strengthen CI

**Files:**
- Modify: `.github/workflows/admin-android-check.yml`
- Modify: `scripts/admin_control_integration_audit.py`
- Modify or replace: `tests/admin-auth-recovery-contract.test.mjs`
- Modify or replace: `tests/admin-webview-input-focus.test.mjs`

**Interfaces:**
- CI must enforce the native security invariants rather than the retired WebView handoff.

- [ ] Change the least-privilege gate to fail if `AdminActivity` contains `WebView`, `evaluateJavascript`, `installNativeAdminSession`, `loadUrl`, or `/admin/native-logout`.
- [ ] Keep the exact-one-bridge assertion for the native Turnstile challenge and its file/content/DOM-storage restrictions.
- [ ] Remove Android requirements for `nativeAuthMode`, `nativeSessionGate`, and browser session installation; retain browser-auth checks only as browser checks.
- [ ] Update integration audit to require native session-store ownership and Compose workspace entry.
- [ ] Update Node contract tests to encode the new architecture.
- [ ] Run the Node contracts and Admin Gradle test/lint/assemble commands.

### Task 6: Version, PR, full verification and immutable release

**Files:**
- Modify: `android/adminapp/build.gradle`
- Later generated metadata: `admin/admin-update.json` after the immutable release exists.

**Interfaces:**
- Produces: signed Morley Admin 0.1.41 (42) APK and matching protected OTA metadata.

- [ ] Bump to `versionCode 42`, `versionName '0.1.41'`.
- [ ] Open a PR with the repository UI checklist and explicit native-auth security invariants.
- [ ] Wait for Admin Android, security, integration, quality, parity, web-smoke and version-governance gates; repair any failing contract rather than bypassing it.
- [ ] Merge only after meaningful gates are green.
- [ ] Verify the signed release workflow publishes a new immutable `admin-v0.1.41` APK, stable signer verification succeeds and SHA-256 is recorded.
- [ ] Publish exact 0.1.41 OTA metadata to protected `main`; if duplicate-tag automation collides, publish metadata through a user-owned branch without reusing/replacing the release.
- [ ] Verify OTA deployment/logs before declaring automatic update delivery complete.
- [ ] Physical Samsung acceptance is the final external verification: native login -> native dashboard remains open -> workspace navigation -> background/foreground -> refresh -> sign out -> Back cannot reopen authenticated workspace.

## Plan self-review

- Coverage: all spec security invariants, native session ownership, role gating, error recovery, OTA release and physical acceptance are represented.
- Placeholder scan: no implementation placeholder is required to execute a task; existing component signatures are named explicitly.
- Type consistency: all tasks use the existing mutable `AdminSession` token fields and the single `AdminSessionStore` interface defined in Task 1.
