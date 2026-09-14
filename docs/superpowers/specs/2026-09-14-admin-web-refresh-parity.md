# Morley Admin web refresh parity

## Defect
The rebuilt Admin web/mobile-web header exposes a Refresh action, but the parity runtime re-clicked the active legacy tab instead of reloading live workspace data. Legacy tab clicks only change panel visibility. Production-health refresh separately queried tickets and errors, so full-access Admin data such as users, devices, config, audit, metrics, notifications and announcements could remain stale. Staff support-only Refresh also did not reload the support queue.

## Expected behavior
Match the native Admin Refresh intent with the existing browser data owners:
- Admin/Manager sessions reuse the existing authenticated `refreshAll()` routine to reload the complete Admin snapshot, then update parity health mirrors/status.
- Staff sessions remain support-only and reuse the existing `loadSupportTickets()` routine to reload the support queue without loading privileged Admin modules.

## Safety boundary
No Auth/RLS/role policy, Supabase schema, Guardian authority, pricing, release identity or native Android behavior changes. The repair reuses existing authorized browser refresh functions rather than introducing new backend calls or duplicate data ownership. Staff continues to avoid the privileged core Admin runtime.

## Verification
TDD established RED before implementation: the protected Admin least-privilege workflow failed when the regression contract required a real data-reload delegation but the parity Refresh handler still only re-clicked a tab. The production repair is limited to the parity Refresh handler plus a cache-key bump so deployed browsers receive the corrected runtime.
