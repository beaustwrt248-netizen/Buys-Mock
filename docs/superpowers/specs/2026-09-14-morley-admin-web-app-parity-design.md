# Morley Admin Web + Mobile-Web App Parity Design

## Goal
Rebuild the Morley Admin desktop website and mobile website so the Android Morley Admin app is the UX and feature source of truth, while preserving the existing audited backend/auth/security boundaries.

## Source of truth
The Android Admin app's native workspace model is canonical for navigation, hierarchy, naming and core operational behavior. The web implementation must expose the same primary workspaces: Overview, Support, Catalogue, Production health, Guardian, Notifications, Users & devices, Staff alerts, Safe controls, Audit, and Release.

## Architecture
The existing browser authentication bootstrap remains the entry boundary. After authentication, `workspace.html` loads a rebuilt `workspace-template.html` shell and the existing feature modules continue to own their audited backend actions. A new app-parity controller maps native workspace names to existing web feature modules so backend behavior is reused rather than duplicated.

Desktop uses a persistent app-like sidebar plus content workspace. Mobile web uses the same DOM and controller, switching below 820px to a compact header and horizontally scrollable workspace selector / stacked cards. There is one responsive product, not separate desktop and mobile implementations.

## Visual model
Match the Android app's hierarchy: compact MORLEY ADMIN identity, signed-in staff name/role, live-data status, refresh/sign-out actions, strong workspace headings, metric cards, and full-width action/workspace cards. Keep the existing Morley dark/light design tokens where they do not conflict with app parity, but remove the legacy desktop-only tab-strip feel.

## Workspace mapping
- Overview: metrics and app-like workspace launcher cards.
- Support: existing support ticket queue/detail/reply functionality.
- Catalogue: existing central pricing/catalogue functionality, presented as the app's Catalogue workspace.
- Production health: read-only support/error/device health summary.
- Guardian: existing Guardian control centre embedded as a first-class Admin workspace entry, not a visually detached legacy link.
- Notifications: existing targeted notification composer/history.
- Users & devices: account/role controls, invites, password-reset related controls already present, plus registered devices.
- Staff alerts: existing announcements publishing/current alerts.
- Safe controls: existing maintenance/OTA-safe feature controls, keeping server-side restrictions.
- Audit: existing privileged audit timeline/triage.
- Release: existing verified OTA metadata, rollout health and support policy controls.

## Auth and permissions
Keep `browser-auth-bootstrap.js`, `login-security.js`, `auth-boundary.js`, current Supabase profile authorization, and role gating. Staff/manager/admin access remains bounded by existing policies; the rebuild must not create a client-side bypass. Protected actions remain server-audited and Guardian-governed.

## Data and errors
The workspace header must show loading, last refresh, recoverable error, and signed-out states similar to the Android app. Existing module-specific refreshes remain functional. The web shell must not discard authenticated context while replacing the workspace template.

## Mobile web
At narrow widths, navigation becomes touch-friendly and compact, cards become single-column, controls remain >=44px touch targets, horizontal overflow is prevented except the explicit workspace selector, and all feature panels remain available without a separate mobile-only route.

## Compatibility
Preserve existing element IDs used by feature modules unless a test proves a safe migration. This avoids rewriting reliable backend integrations merely for layout parity. Existing deep links such as `guardian.html` may remain for compatibility, but the primary Admin UX must present Guardian within the unified workspace model.

## Testing
Add contract tests that verify:
1. All Android-native workspaces have corresponding web workspace controls/panels.
2. Required legacy element IDs consumed by existing feature modules are retained.
3. The responsive shell includes mobile-specific navigation behavior without hiding functionality.
4. Auth bootstrap/session scripts remain present and ordered before privileged feature modules.
5. Existing Admin web/auth tests still pass.

## Deployment
Deploy only after branch checks pass. The Admin Pages deployment workflow remains the release path; no direct destructive change to `main` is required for implementation.