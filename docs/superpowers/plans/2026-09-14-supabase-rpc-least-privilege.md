# Supabase RPC Least-Privilege Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the authenticated-callable Supabase `SECURITY DEFINER` RPC boundary for issue #2033 without breaking legitimate Morley Admin or Guardian browser workflows or broadening access to server-only tables.

**Architecture:** Preserve the existing browser-required RPC model and current `SECURITY DEFINER` execution boundary. Make the one confirmed authorization gap fail closed (`guardian_report_diagnostic` must require an enabled profile), explicitly codify `PUBLIC`/`anon` revocation and `authenticated` execution for the 13 reviewed RPCs, and add CI-owned contracts that prevent future ACL, role-boundary, Guardian, or RLS-exposure drift. Do not rewrite already-correct privileged function bodies merely to silence an advisor.

**Tech Stack:** PostgreSQL 15 / Supabase migrations and ACL metadata, Node.js built-in test runner, GitHub Actions, Supabase security advisor.

**Spec:** `docs/superpowers/specs/2026-09-14-supabase-rpc-least-privilege-design.md`

## Global Constraints

- Preserve intentional browser RPC entry points; do not blanket-revoke `authenticated` execution.
- Do not convert the reviewed RPCs wholesale to `SECURITY INVOKER`.
- `PUBLIC` and `anon` must not have `EXECUTE` on any of the 13 reviewed RPCs.
- Do not grant `service_role` execution unless a real server caller is proven to require it.
- Privileged actions must reject missing users, disabled profiles, Staff users, and unknown roles before protected state changes.
- Manager invite restrictions and Admin-only privileged exceptions must remain unchanged.
- Guardian human-approval, candidate-review, isolated-test, merge, deployment, and kill-switch boundaries must remain unchanged or stricter.
- The 17 RLS/no-policy tables remain server-only; do not add browser-facing policies or table grants.
- Leaked-password protection is a separate Auth setting and is not changed by this plan.
- No pricing authority, recovery authority, OAuth credential persistence, or unrelated schema behavior changes are permitted.

---

### Task 1: Add the CI-owned least-privilege contract and prove RED

**Files:**
- Create: `tests/supabase-rpc-least-privilege-contract.test.mjs`
- Modify: `.github/workflows/quality-gate.yml`

**Interfaces:**
- Consumes: approved spec role matrix and the future migration path `supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql`.
- Produces: a dedicated Node contract executed by the existing B&L Morley Quality Gate.

- [ ] **Step 1: Create the failing contract test**

Create `tests/supabase-rpc-least-privilege-contract.test.mjs` with this exact structure:

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationPath = new URL(
  '../supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql',
  import.meta.url,
);

const rpcNames = [
  'admin_create_download_invite',
  'admin_create_team_invite',
  'admin_inventory_create',
  'admin_inventory_record_sale',
  'admin_inventory_set_status',
  'admin_reissue_team_invite',
  'admin_revoke_download_invite',
  'admin_revoke_team_invite',
  'guardian_decide_incident',
  'guardian_decide_repair',
  'guardian_report_diagnostic',
  'guardian_set_agent_controls',
  'guardian_set_controls',
];

test('approved RPC hardening migration exists', () => {
  assert.ok(fs.existsSync(migrationPath), 'missing approved RPC hardening migration');
});

test('all reviewed RPCs explicitly deny PUBLIC and anon while retaining authenticated browser entry', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8').toLowerCase();
  for (const name of rpcNames) {
    assert.match(sql, new RegExp(`revoke\\s+execute\\s+on\\s+function\\s+public\\.${name}\\(`));
    assert.match(sql, new RegExp(`grant\\s+execute\\s+on\\s+function\\s+public\\.${name}\\(`));
  }
  assert.match(sql, /from\s+public\s*,\s*anon/i);
  assert.match(sql, /to\s+authenticated/i);
  assert.doesNotMatch(sql, /grant\s+execute[^;]+to\s+service_role/i);
});

test('Guardian diagnostics require an enabled profile, not only a non-null auth uid', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');
  assert.match(sql, /guardian_report_diagnostic/);
  assert.match(sql, /from\s+public\.profiles\s+p/i);
  assert.match(sql, /p\.id\s*=\s*auth\.uid\(\)/i);
  assert.match(sql, /p\.is_enabled\s*=\s*true/i);
  assert.match(sql, /Authentication required/i);
});

test('migration does not broaden browser table access or weaken Guardian controls', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');
  assert.doesNotMatch(sql, /create\s+policy/i);
  assert.doesNotMatch(sql, /grant\s+(select|insert|update|delete|all)\s+on\s+(table\s+)?public\./i);
  assert.doesNotMatch(sql, /disable\s+row\s+level\s+security/i);
  assert.doesNotMatch(sql, /security\s+invoker/i);
  assert.match(sql, /require_human_for_code\s*=\s*true/i);
  assert.match(sql, /Only an Admin can disengage the Guardian kill switch/i);
});
```

- [ ] **Step 2: Wire the contract into the existing quality gate**

Immediately after the `Morley ecosystem contract` step in `.github/workflows/quality-gate.yml`, add:

```yaml
      - name: Supabase RPC least-privilege contract
        run: node --test tests/supabase-rpc-least-privilege-contract.test.mjs
```

- [ ] **Step 3: Run the focused test and verify RED**

Run:

```bash
node --test tests/supabase-rpc-least-privilege-contract.test.mjs
```

Expected: FAIL at `approved RPC hardening migration exists` with `missing approved RPC hardening migration`.

- [ ] **Step 4: Commit the RED contract only**

```bash
git add tests/supabase-rpc-least-privilege-contract.test.mjs .github/workflows/quality-gate.yml
git commit -m "test: define Supabase RPC least-privilege contract"
```

---

### Task 2: Harden Guardian diagnostics and codify the 13 RPC ACLs

**Files:**
- Create: `supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql`
- Test: `tests/supabase-rpc-least-privilege-contract.test.mjs`

**Interfaces:**
- Consumes: the 13 existing public RPC signatures and the existing `private.is_admin_or_manager()` / `private.is_admin()` helpers.
- Produces: explicit ACL state plus an enabled-profile guard in `guardian_report_diagnostic` while preserving its telemetry, metadata redaction, fingerprinting, deduplication, and incident-state behavior.

- [ ] **Step 1: Add a precondition block that aborts on missing reviewed RPCs**

Start the migration with a `DO` block that checks all 13 `regprocedure` signatures resolve. Use `to_regprocedure(...)` and raise an exception if any is missing so migration drift fails closed rather than silently applying a partial ACL set.

Use the exact signatures below:

```sql
public.admin_create_download_invite(text,text,text,text,text,timestamptz)
public.admin_create_team_invite(text,text,text,text,timestamptz)
public.admin_inventory_create(text,text,numeric,numeric,bigint,uuid,text,text,text,text)
public.admin_inventory_record_sale(uuid,numeric,numeric,numeric,text,text,timestamptz)
public.admin_inventory_set_status(uuid,text)
public.admin_reissue_team_invite(uuid,text,timestamptz)
public.admin_revoke_download_invite(uuid)
public.admin_revoke_team_invite(uuid)
public.guardian_decide_incident(uuid,text)
public.guardian_decide_repair(uuid,text)
public.guardian_report_diagnostic(text,text,text,text,text,text,jsonb)
public.guardian_set_agent_controls(boolean,boolean,boolean,text)
public.guardian_set_controls(boolean,boolean,text,text,boolean,boolean,numeric,integer,boolean,boolean,text)
```

- [ ] **Step 2: Replace only the opening authorization guard in `guardian_report_diagnostic`**

Keep the existing function signature, `SECURITY DEFINER`, explicit search path, metadata redaction, server-derived fingerprint, deduplication, and insert/update logic unchanged. Replace the current single check:

```sql
if auth.uid() is null then raise exception 'Authentication required'; end if;
```

with:

```sql
if auth.uid() is null or not exists (
  select 1
  from public.profiles p
  where p.id = auth.uid()
    and p.is_enabled = true
) then
  raise exception 'Authentication required';
end if;
```

Retain the existing Guardian safety behavior in the same recreated function, including `require_human_for_code` invariants in the surrounding Guardian system and the fact that diagnostic submission cannot approve incidents, approve repairs, merge, or deploy.

- [ ] **Step 3: Explicitly normalize EXECUTE ACLs for all 13 reviewed RPCs**

For every exact signature from Step 1, issue both statements:

```sql
revoke execute on function public.<name>(<exact-arg-types>) from public, anon;
grant execute on function public.<name>(<exact-arg-types>) to authenticated;
```

Do not grant `service_role`. Do not change schema privileges. Do not add table grants.

- [ ] **Step 4: Preserve privileged role behavior by assertion comments and unchanged bodies**

The migration must retain these existing function-specific constraints in the live definitions it recreates or leaves unchanged:

```text
admin_create_download_invite:
  Manager -> Staff only; Admin-channel and Manager/Admin invites remain Admin-only.
admin_create_team_invite / reissue / revoke:
  Managers can manage Staff invites only, with existing ownership restrictions.
admin_inventory_create / record_sale / set_status:
  private.is_admin_or_manager() remains the authority gate before protected state change.
guardian_decide_incident / guardian_decide_repair:
  private.is_admin_or_manager() remains required before approval-state transitions.
guardian_set_agent_controls:
  enabled Admin/Manager role remains required.
guardian_set_controls:
  enabled Admin/Manager role remains required; only Admin can disengage an engaged kill switch.
```

Do not recreate the 12 already-correct RPC bodies unless a signature-level ACL statement requires no body change; minimizing function-body churn is deliberate.

- [ ] **Step 5: Run the focused contract and verify GREEN**

Run:

```bash
node --test tests/supabase-rpc-least-privilege-contract.test.mjs
```

Expected: all tests PASS.

- [ ] **Step 6: Run the existing ecosystem contract**

Run:

```bash
node --test tests/morley-ecosystem-contract.test.mjs
```

Expected: PASS with no Guardian, recovery, scheduler, catalogue, or index-contract regressions.

- [ ] **Step 7: Commit the minimal migration**

```bash
git add supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql
git commit -m "security: harden authenticated definer RPC boundary"
```

---

### Task 3: Add repository-level regression assertions for server-only table posture

**Files:**
- Modify: `tests/supabase-rpc-least-privilege-contract.test.mjs`
- Test: `tests/supabase-rpc-least-privilege-contract.test.mjs`

**Interfaces:**
- Consumes: migration SQL from Task 2.
- Produces: a durable contract that this change never introduces policies/table grants merely to clear `rls_enabled_no_policy` findings.

- [ ] **Step 1: Add representative server-only table assertions**

Append this test:

```js
test('server-only advisor tables remain intentionally outside browser grants', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8').toLowerCase();
  for (const table of [
    'device_buy_prices',
    'device_buy_price_history',
    'nova_catalog_audit_queue',
    'nova_catalog_audit_findings',
    'nova_catalog_audit_runs',
    'nova_knowledge_items',
    'nova_knowledge_chunks',
    'user_drive_backup_keys',
    'user_drive_backup_master_keys',
  ]) {
    assert.doesNotMatch(sql, new RegExp(`grant\\s+(select|insert|update|delete|all)[^;]+${table}`));
    assert.doesNotMatch(sql, new RegExp(`create\\s+policy[^;]+${table}`));
  }
});
```

- [ ] **Step 2: Run the contract**

```bash
node --test tests/supabase-rpc-least-privilege-contract.test.mjs
```

Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add tests/supabase-rpc-least-privilege-contract.test.mjs
git commit -m "test: lock server-only Supabase table posture"
```

---

### Task 4: Open the implementation PR and run all exact-head protected gates

**Files:**
- No production file changes in this task.
- Review: full PR diff.

**Interfaces:**
- Consumes: Tasks 1-3 commits.
- Produces: a reviewable PR whose exact head has complete CI evidence.

- [ ] **Step 1: Create the implementation PR from a fresh branch based on current `main`**

Use a branch such as:

```text
security/supabase-rpc-least-privilege-20260914
```

If `main` has advanced since the design/plan branch, branch from current `main` and copy only the approved implementation/test changes; do not merge unrelated design-branch drift into production code.

- [ ] **Step 2: Require the following exact-head workflows to complete successfully**

Verify GREEN for:

```text
B&L Morley Quality Gate
Repository Security Audit
Morley Ultimate Parity Gate
Admin Control Integration Audit
Recovery Backup Contract
Morley Restore Point Capture
Repository Path Stability
Pricing Migration Supersession Contract
Catalogue Shared Model Classification Contract
Morley Email Contract
```

If a workflow is cancelled externally, rerun it; cancellation is not a pass.

- [ ] **Step 3: Review the full diff before merge**

The changed-file set must contain only the approved security migration, dedicated contract test, and quality-gate wiring unless a directly required test-only file is added during implementation. Reject any unexpected Auth setting, RLS policy, table grant, pricing, recovery, Guardian approval, deployment, or unrelated application change.

- [ ] **Step 4: Merge only with the verified head SHA**

Use expected-head protection on merge so a moving PR head cannot bypass the reviewed CI evidence.

---

### Task 5: Apply the exact migration to production and verify the live boundary

**Files:**
- Apply: `supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql`
- No additional production edits.

**Interfaces:**
- Consumes: merged migration from Task 4.
- Produces: verified production ACL/function state and fresh advisor evidence.

- [ ] **Step 1: Apply the exact repository migration using the Supabase migration API**

Migration name:

```text
harden_authenticated_security_definer_rpcs
```

Do not execute an ad-hoc variant of the SQL.

- [ ] **Step 2: Verify all 13 live ACLs from `pg_proc`**

Run a read-only query that returns, for each reviewed function:

```text
proname
prosecdef
authenticated_execute
anon_execute
public_execute
service_role_execute
```

Expected for all 13:

```text
prosecdef = true
authenticated_execute = true
anon_execute = false
public_execute = false
service_role_execute = false
```

- [ ] **Step 3: Verify `guardian_report_diagnostic` includes the enabled-profile check**

Inspect `pg_get_functiondef(...)` and confirm it contains both:

```sql
p.id = auth.uid()
p.is_enabled = true
```

and still contains the existing metadata redaction patterns and server-generated fingerprint logic.

- [ ] **Step 4: Verify server-only tables remain inaccessible to browser roles**

Use `has_table_privilege` for both `anon` and `authenticated` against representative tables:

```text
device_buy_prices
device_buy_price_history
nova_catalog_audit_queue
nova_knowledge_items
nova_knowledge_chunks
user_drive_backup_keys
user_drive_backup_master_keys
```

Expected: no SELECT/INSERT/UPDATE/DELETE privilege for either browser role.

- [ ] **Step 5: Run negative canaries without committing business mutations**

At minimum verify unauthenticated calls to the reviewed RPC surface fail. For authenticated role-negative canaries, use existing test fixtures or a transaction that rolls back every write; do not create persistent users or alter real staff roles merely to test denial. Confirm:

```text
Staff cannot invoke inventory/Guardian privileged state changes.
Disabled or missing-profile users cannot invoke guardian_report_diagnostic.
Manager cannot disengage an engaged Guardian kill switch.
Manager invite restrictions remain unchanged.
```

If no safe existing fixture exists for one role-negative case, leave that case covered by repository contract plus function-definition inspection rather than mutating production identity state.

- [ ] **Step 6: Re-run Supabase advisors**

Run both security and performance advisors. Expected security interpretation:

```text
The authenticated SECURITY DEFINER lint may remain because authenticated browser execution is intentional.
The 17 RLS/no-policy INFO findings may remain because those tables are intentionally server-only.
Leaked-password protection may remain WARN until the separate Auth setting is changed through an authorized Auth-management surface.
No new security-advisor category should appear because of this migration.
```

- [ ] **Step 7: Add production evidence to issue #2033**

Record merge SHA, migration version, live ACL results, enabled-profile diagnostic proof, server-only table-grant proof, exact-head CI results, and advisor classification. Keep #2033 open if leaked-password protection remains unresolved.

---

### Task 6: Final verification and rollback readiness

**Files:**
- Review: `supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql`
- Review: issue #2033 evidence comment.

**Interfaces:**
- Consumes: production verification from Task 5.
- Produces: completion decision for the RPC-hardening sub-lane without falsely closing unrelated Auth work.

- [ ] **Step 1: Verify rollback instructions are explicit in the migration comments**

The migration comments must record the pre-change ACL posture:

```text
postgres EXECUTE
authenticated EXECUTE
no anon EXECUTE
no PUBLIC EXECUTE
no service_role EXECUTE
```

and state that rollback of `guardian_report_diagnostic` restores only its previous authentication guard, never RLS/table grants or Guardian approval weakening.

- [ ] **Step 2: Recheck exact post-merge `main` workflows**

Confirm no post-merge push workflow is failed, queued indefinitely, or still running before claiming the RPC-hardening lane complete.

- [ ] **Step 3: Classify issue #2033**

If RPC hardening and table-posture verification are complete but leaked-password protection is still disabled, leave #2033 open (or split the remaining Auth setting into a dedicated protected issue and close #2033 only if the project’s issue convention prefers one concern per issue). Do not claim the entire security-advisor set is cleared while that Auth WARN remains.
