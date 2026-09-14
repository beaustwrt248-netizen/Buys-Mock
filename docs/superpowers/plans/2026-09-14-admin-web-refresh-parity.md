# Morley Admin web refresh parity plan

1. Add a regression assertion to the protected Admin auth/parity test proving web Refresh delegates to the existing full Admin snapshot reload.
2. Open a draft PR with the test-only head and require the Admin least-privilege gate to fail for the missing bridge/call.
3. Expose the existing `refreshAll` function through a minimal frozen `window.MorleyAdminRuntime` object after function definition.
4. Change the rebuilt parity header Refresh handler to await `MorleyAdminRuntime.refreshAll()` for full-access Admin/Manager sessions, then refresh parity health mirrors/status.
5. Preserve Staff support-only isolation; do not load the privileged core runtime or broaden staff data access.
6. Re-run the protected Admin gate and repository security/quality/parity checks; review the exact diff before merge.
