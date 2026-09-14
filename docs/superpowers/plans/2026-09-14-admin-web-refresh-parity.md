# Morley Admin web refresh parity plan

1. Add a regression assertion to the protected Admin auth/parity test proving web Refresh delegates to real data reloads rather than tab visibility changes.
2. Open a draft PR with the test-only head and require the Admin least-privilege gate to fail on the pre-fix behavior.
3. Reuse the existing global `refreshAll()` browser function for full-access Admin/Manager sessions; do not create a second snapshot/data owner.
4. Reuse the existing `loadSupportTickets()` function for Staff so support-only Refresh reloads the queue without loading privileged Admin modules.
5. Change the rebuilt parity header Refresh handler to await the appropriate role-scoped data owner, preserve the active workspace, refresh health mirrors where relevant, expose loading/error state, and prevent concurrent refresh clicks.
6. Bump the parity runtime cache key in the authorized workspace loader so deployed browsers cannot retain the stale implementation.
7. Preserve Staff support-only isolation and all existing Auth/RLS/Guardian/pricing/release boundaries.
8. Re-run the protected Admin gate and repository security/quality/parity/web-smoke checks; review the exact diff before merge.
