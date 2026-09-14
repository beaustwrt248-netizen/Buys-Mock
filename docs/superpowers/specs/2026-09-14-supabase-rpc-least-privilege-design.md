# Supabase RPC Least-Privilege Hardening Design

Date: 2026-09-14
Issue: #2033
Status: Design approved; implementation not yet started

## Goal

Reduce the attack surface represented by authenticated-callable `SECURITY DEFINER` RPCs without breaking legitimate Morley Admin or Guardian browser flows, weakening Guardian approval boundaries, broadening table access, or changing unrelated Auth/RLS behavior.

This design intentionally does **not** attempt to silence every Supabase advisor finding. Advisor output is evidence to review, not an instruction to add policies, revoke browser access indiscriminately, or convert functions blindly.

## Current production evidence

A fresh production advisor pass reports:

- 13 authenticated-callable `SECURITY DEFINER` functions.
- 17 RLS-enabled tables with no policies.
- leaked-password protection disabled.

Read-only production inspection established that the 17 RLS/no-policy tables currently grant no table operations to `anon` or `authenticated`; they are therefore server/internal data surfaces rather than missing client policies. Adding permissive policies would broaden exposure and is out of scope.

The 13 RPCs all have `EXECUTE` for `authenticated`, no `anon` execution, and explicit in-function authorization checks. The warning is therefore not proof of an active bypass, but the definer boundary remains security-sensitive and should be made uniform, testable, and fail-closed.

## Design principles

1. **Preserve intentional browser RPC entry points.** If the browser legitimately invokes a privileged workflow, keep the RPC callable by `authenticated`; authorization remains inside the function and must fail closed.
2. **No blanket `SECURITY INVOKER` conversion.** These functions exist partly to perform controlled writes across otherwise inaccessible tables. Converting them without redesigning grants would either break workflows or tempt broader table permissions.
3. **No blanket EXECUTE revocation.** Revoke `anon`/`public` where applicable, but do not remove `authenticated` from browser-required RPCs.
4. **Enabled-profile checks are mandatory for privileged actions.** Role-bearing functions must reject missing users, disabled profiles, and roles outside the explicit allowlist before any protected read/write.
5. **Function-specific constraints remain stricter than role checks.** Manager-to-Staff invite restrictions, Admin-only privileged invites, Guardian kill-switch Admin-only disengagement, and human-approval transitions must remain explicit.
6. **Guardian protections cannot be weakened.** No function may bypass the existing human-approval, candidate-review, isolated-test, merge, or deployment boundaries.
7. **Server-only RLS tables remain server-only.** No new client policies or grants are introduced merely to clear the `rls_enabled_no_policy` informational lint.
8. **Leaked-password protection is a separate Auth setting.** It is not changed by this database/RPC implementation and remains a distinct protected action.

## RPC role matrix

### Any authenticated, enabled user

`guardian_report_diagnostic(...)`

Purpose: client runtime telemetry. It must require a signed-in user, sanitize metadata, preserve server-derived fingerprinting, and never perform privileged repair/approval actions. Because diagnostics are a user-facing telemetry path, this RPC remains executable by `authenticated`.

### Admin or Manager

- `admin_inventory_create(...)`
- `admin_inventory_record_sale(...)`
- `admin_inventory_set_status(...)`
- `guardian_decide_incident(...)`
- `guardian_decide_repair(...)`
- `guardian_set_agent_controls(...)`
- `guardian_set_controls(...)`

These must reject anonymous callers, disabled profiles, Staff users, unknown roles, and missing profiles before protected state changes.

`guardian_set_controls(...)` retains the additional rule that only Admin may disengage an already-engaged kill switch.

### Admin or Manager with target/action restrictions

- `admin_create_team_invite(...)`
- `admin_reissue_team_invite(...)`
- `admin_revoke_team_invite(...)`
- `admin_create_download_invite(...)`
- `admin_revoke_download_invite(...)`

Manager restrictions remain fail-closed:

- Managers may create only Staff invitations.
- Managers may manage only their own Staff invitations where ownership is part of the existing contract.
- Managers may not create Admin-app download invitations.
- Managers may not create Manager/Admin privileged invitations.
- Admin retains the broader existing authority; no new role is introduced.

## Authorization architecture

### Shared private helpers

Implementation should converge privileged role checks on private, non-exposed helper functions rather than maintaining subtly different role-resolution patterns in every RPC.

The preferred helper boundary is:

- resolve `auth.uid()`;
- read `public.profiles` for that exact user;
- require `is_enabled = true`;
- return a normalized role or raise an authorization error;
- provide a narrow `private.require_admin_or_manager()`-style wrapper for privileged RPCs;
- optionally provide `private.require_admin()` where function-specific Admin-only operations need it.

Existing safe helpers may be reused if their live definitions already satisfy those requirements. Do not create parallel role systems if the current private helpers are correct.

### Function bodies

Each warned RPC must be reviewed and normalized so authorization happens before protected table reads that disclose sensitive state and before all writes. Input validation remains after authorization unless validation itself is needed before any database access.

Functions with object ownership or target-role restrictions keep those checks inside the function after caller authorization and after locking the target row where concurrency matters.

### ACLs and schema exposure

For every affected RPC:

- `PUBLIC` and `anon` must not have `EXECUTE`.
- `authenticated` may retain `EXECUTE` only when the browser/app has a legitimate direct call path.
- `service_role` execution is added only if an actual server caller requires it; do not add it pre-emptively.
- helper functions in `private` must not become exposed through the Data API.
- search paths remain explicit and minimal to reduce object-shadowing risk.

## RLS/no-policy findings

The 17 informational findings are intentionally classified as **server-only fail-closed tables** unless later evidence proves otherwise.

Implementation for this design must not add RLS policies or authenticated table grants to them. Regression coverage should instead assert that selected critical tables remain inaccessible to `anon` and `authenticated`, especially:

- protected buy pricing/history;
- Nova catalogue audit queue/findings/runs;
- Nova knowledge internals;
- per-user backup key/master-key material.

This preserves the current defense-in-depth posture: RLS enabled plus no client policies plus no client table grants.

## Testing strategy

Implementation follows TDD and must add failing security contracts before production SQL changes.

Required test classes:

1. **ACL contract tests**
   - no warned RPC grants `EXECUTE` to `PUBLIC` or `anon`;
   - browser-required RPCs retain only the intended execution roles;
   - private helpers are not exposed as public RPCs.

2. **Role-negative tests**
   - Staff cannot create inventory, record sales, change inventory state, decide Guardian incidents/repairs, or alter Guardian controls;
   - disabled Admin/Manager profiles are denied;
   - missing-profile authenticated users are denied.

3. **Manager-boundary tests**
   - Manager can create Staff invite where allowed;
   - Manager cannot create Manager/Admin invite;
   - Manager cannot create Admin-channel download invite;
   - Manager cannot revoke/reissue another manager's restricted invite where ownership is required;
   - Manager cannot disengage an engaged Guardian kill switch.

4. **Admin-success tests**
   - Admin retains current legitimate privileged paths.
   - Existing input validation and audit logging remain intact.

5. **Guardian diagnostic tests**
   - authenticated user can submit a diagnostic;
   - anonymous caller cannot;
   - diagnostic metadata redaction remains enforced;
   - diagnostic RPC cannot trigger approval, repair, merge, or deployment actions.

6. **Server-only table exposure tests**
   - representative RLS/no-policy tables remain inaccessible to `anon`/`authenticated`;
   - service/backend access required by active Edge Functions remains intact.

7. **Repository-wide gates**
   - Repository Security Audit;
   - B&L Morley Quality Gate;
   - Morley Ultimate Parity Gate;
   - Admin Control Integration Audit;
   - Recovery Backup Contract;
   - pricing/catalogue/path/email contracts;
   - any dedicated Supabase security contract added by implementation.

## Deployment sequence

1. Add RED tests/contracts on a feature branch.
2. Implement helper/function/ACL migration minimally until tests pass.
3. Run all exact-head protected CI gates.
4. Review full migration diff for unrelated DDL, grants, policies, Auth changes, or Guardian boundary drift.
5. Merge only after all required checks pass.
6. Apply the exact repository migration to production.
7. Verify production ACLs and function definitions directly from `pg_proc`/ACL metadata.
8. Exercise negative authorization canaries without mutating protected business state where possible.
9. Re-run Supabase security advisors and classify remaining findings rather than chasing zero warnings.
10. Keep issue #2033 open if leaked-password protection or other independently protected Auth work remains outstanding.

## Rollback

The migration must contain enough information to restore pre-change EXECUTE ACLs and function definitions if a legitimate Admin/Guardian caller breaks. Rollback must not broaden access beyond the pre-change state.

No rollback may disable RLS, grant table access to `authenticated`, remove Guardian approval checks, or persist privileged credentials client-side.

## Rejected alternatives

### Convert all warned functions to SECURITY INVOKER

Rejected because several workflows intentionally write to tables that remain inaccessible to browser roles. Making them invoker functions would either break production flows or require broader table grants, which increases attack surface.

### Revoke authenticated EXECUTE from all warned functions

Rejected because multiple Admin and Guardian browser workflows legitimately call these RPCs directly. Blanket revocation would break supported functionality and push pressure toward less controlled alternatives.

### Add RLS policies to clear all 17 informational findings

Rejected because live grants show those tables are intentionally not client-accessible. Adding policies for the sake of the advisor would create new exposure rather than remediate one.

## Success criteria

This design is complete when:

- every privileged warned RPC has a uniform, explicit, enabled-profile authorization boundary;
- Staff/disabled/missing-profile callers are proven unable to cross Admin/Manager boundaries;
- Manager-specific restrictions remain intact;
- Guardian diagnostic telemetry remains usable by signed-in users without gaining privileged capabilities;
- Guardian human-approval and protected repair boundaries are unchanged or stricter;
- the 17 server-only RLS/no-policy tables remain inaccessible to browser roles;
- no Auth setting, pricing authority, recovery authority, or unrelated schema behavior is changed;
- production verification matches the repository migration exactly.
