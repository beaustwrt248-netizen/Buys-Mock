# Morley Admin Native Rebuild Design

## Status
Approved direction from the 2026-09-12 Admin auth incident: rebuild the Android Admin shell as a native Compose application and stop using the authenticated website as the app workspace.

## Problem
Morley Admin 0.1.38–0.1.40 authenticate successfully in the native login activity but can fall back to the website login because the privileged Android workspace is a WebView. The app currently transfers Supabase access/refresh tokens into JavaScript after the Admin page loads and then watches a browser-side state flag. This couples Android authentication to browser initialization, storage, routing and the website's own login lifecycle. Repeated timing and bootstrap changes have not eliminated the loop.

The repository already contains a substantial native Admin implementation and current native Compose panels/APIs for support operations, Guardian, notifications, audit, user management, team invites, password reset, update handling and role/access policy. The rebuild will make those native components authoritative instead of wrapping the Admin website.

## Decision
Build the Android Admin app as a fully native authenticated shell. The only WebView permitted in the normal app is the tightly-scoped Cloudflare Turnstile challenge used during login. The authenticated workspace must not contain a WebView, must not load `buyshub.me/admin/`, and must not inject Supabase sessions into JavaScript.

The website remains an independent Admin client. Its browser login and session lifecycle are maintained separately and must not be used as a dependency of Android authentication.

## Considered approaches

### 1. Keep the WebView workspace and add more handoff retries
Rejected. This preserves the failing architecture: two independent auth runtimes and a race between Android token ownership and browser session initialization.

### 2. Native login plus authenticated WebView with a custom cookie/session bridge
Rejected. It reduces some timing risk but still makes the Android app dependent on website routing, storage semantics and future browser-auth changes.

### 3. Fully native Android workspace using the existing Supabase REST/API layer
Selected. It removes the cross-runtime handoff entirely, keeps backend authorization/RLS authoritative, reuses the current native operational panels, and gives Android one session owner.

## Architecture

### Launcher/update gate
`AdminUpdateGateActivity` remains the launcher and continues to check the signed Admin OTA feed. OTA failure must never lock an administrator out. When no mandatory update blocks entry, it routes to the native authentication gate.

### Authentication
`AdminLoginActivity` remains native Compose. Email/password are native text fields. Turnstile is the only WebView and retains exactly one `AndroidBridge`, scoped only to the challenge page. `AdminApi.signIn(...)` exchanges credentials + captcha token with Supabase, verifies the profile and role, and returns an `AdminSession` only after backend authorization succeeds.

### Native session owner
Introduce a single Android session owner/repository responsible for the in-memory active `AdminSession`, token refresh updates and explicit clearing on sign-out. Access/refresh tokens must not be passed to JavaScript, URLs, browser storage, logs, telemetry or intents after the rebuild.

For the first rebuilt release, the authenticated session is process-scoped rather than silently restored from disk. If Android kills the process, the next launch returns to native sign-in. This is intentionally simpler and safer than adding encrypted persistence during the auth-recovery release. Secure remembered-session persistence can be a later feature after the native shell is proven stable.

### Authenticated workspace
`AdminActivity` becomes a Compose activity. It reads the active native session from the session owner and fails closed to `AdminLoginActivity` if none exists. It loads `AdminApi.load(session)` directly and renders the native dashboard.

The dashboard uses the existing current native panels and policies instead of HTML/JavaScript equivalents:
- Overview / production health from `AdminSnapshot`
- Support queue and protected conversations via `SupportOperationsPanel`
- Guardian via `GuardianPanel`
- Notifications via the current notification panel
- Users/devices and user controls via current user-management/control components
- Team invites via current team-invite components
- Audit via `AuditTimelinePanel`
- Remote configuration / maintenance controls via existing policies/APIs
- Password-reset tooling where current role policy permits
- Release/version adoption visibility using current release/version helpers

Role boundaries remain backend-first. `admin` and `manager` may load the full snapshot where current policy allows; `staff` receives the support-only surface defined by `AdminAppAccessPolicy`.

### Sign-out
Sign-out clears the native session owner, removes in-memory snapshot/workspace state, and navigates to `AdminLoginActivity` with the authenticated activity removed from the back stack. There is no `/admin/native-logout` browser route in the Android app.

### Website isolation
The mobile/desktop website remains at `buyshub.me/admin/` with its own browser auth. Android must not depend on `admin/index.html`, `login-security.js`, browser local storage or `installNativeAdminSession` to enter its workspace.

## UI
The native app keeps the existing Morley Admin visual language: dark navy background, cyan accent, compact operational cards, clear role/status indicators, safe-area handling and mobile-first Compose controls. Reuse the existing native dashboard/panel styling where possible rather than visually redesigning the product during an auth-recovery rebuild.

The authenticated shell should use a native top-level navigation model suitable for the existing workspaces. It must keep actions reachable on a Samsung-sized phone without horizontal clipping, and destructive/privileged actions must retain their current confirmations and role gating.

## Error handling
- Sign-in errors stay on the native login screen and invalidate the current captcha token.
- Unauthorized/disabled profiles never enter the workspace.
- Snapshot load failures show a native recoverable error with Refresh and Sign out; they do not redirect to the website.
- Expired JWTs continue to use `AdminApi` refresh-token handling. If refresh is rejected, the session owner is cleared and the app returns to native login.
- OTA/network failures remain non-blocking unless the OTA metadata explicitly marks an update required.
- Telemetry must not include credentials or tokens.

## Security invariants
1. `AdminActivity` contains no `WebView`, `addJavascriptInterface`, `evaluateJavascript`, session injection or trusted Admin URL routing.
2. The only Android JavaScript bridge is the Turnstile bridge in the login challenge.
3. Access/refresh tokens never cross into web content.
4. Authorization remains enforced by Supabase/RLS and current role policies; UI gating is additive, not authoritative.
5. Sign-out clears all native session state.
6. Recovery builds use the same native authentication and workspace boundaries.
7. Browser Admin and Android Admin authentication are independent clients sharing the backend, not each other's sessions.

## Regression tests and CI contracts
Add tests/contracts that fail if:
- `AdminActivity` imports or constructs `WebView`.
- `AdminActivity` calls `evaluateJavascript`, `loadUrl`, `installNativeAdminSession`, or contains `/admin/native-logout`.
- more than the approved Turnstile login bridge exists in the Admin Android app.
- login can start `AdminActivity` before the native session owner has an authorized session.
- sign-out fails to clear the native session.
- a staff role can access full-snapshot-only workspaces.

Keep the existing Admin unit tests, repository security audit, Admin integration audit, APK signer verification, parity gate, quality gate, version governance and OTA release safety checks green.

## Release plan
The first rebuilt APK gets a new immutable version identity after 0.1.40. It must be signed with the stable Admin signer, published through the protected release process, have matching OTA metadata, and be physically verified on Samsung before the old WebView architecture is considered retired.

Physical acceptance sequence:
1. Launch app and complete native Turnstile.
2. Sign in with an authorized account.
3. Native Admin dashboard appears and remains visible for at least 30 seconds.
4. Navigate between at least Overview, Support, Guardian (if role permits), Users/devices and Audit without any website login appearing.
5. Background/foreground the app and confirm the native workspace remains signed in while the process is alive.
6. Refresh data and exercise one safe read-only workspace.
7. Sign out and confirm native login appears and Back cannot reopen the authenticated workspace.

## Out of scope for the auth-recovery release
- Rebuilding the Supabase backend or database schema.
- Changing production RLS/role semantics unless a failing native feature proves a backend contract mismatch.
- Adding long-term encrypted remembered-session persistence.
- Redesigning the entire Admin visual system.
- Treating the browser Admin login as part of the Android session flow.

The browser Admin login issue remains a separate web repair stream and can be fixed/deployed independently without changing the native Android session contract.
