# Morley Admin web refresh parity

## Defect
The rebuilt Admin web/mobile-web header exposes a Refresh action, but the parity runtime currently re-clicks the active legacy tab instead of reloading the authenticated Admin snapshot. Legacy tab clicks only change panel visibility. Production-health refresh separately queries tickets and errors, so devices and other workspace data can remain stale.

## Expected behavior
Match the native Admin Refresh contract: one bounded authenticated refresh reloads the full Admin snapshot used by Users, Devices, Config, Audit, Metrics, Notifications and Announcements, then updates parity health mirrors/status.

## Safety boundary
No Auth/RLS/role policy, Supabase schema, Guardian authority, pricing, release identity or native Android behavior changes. The fix must reuse the existing authenticated `refreshAll()` routine rather than introduce new backend calls or duplicate data ownership.

## Verification
TDD: first require the core runtime to expose only the existing `refreshAll` routine through a frozen bounded bridge and require the parity Refresh handler to await it. The new assertion must fail on the pre-fix runtime, then pass after the minimal implementation.
