# Supabase RPC Least-Privilege Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Harden the authenticated-callable Supabase `SECURITY DEFINER` RPC boundary for issue #2033 without breaking legitimate Morley Admin or Guardian browser workflows or broadening access to server-only tables.

**Architecture:** Keep the current browser-required `SECURITY DEFINER` model. Fix the one confirmed authorization gap by requiring `guardian_report_diagnostic` callers to have an enabled profile, explicitly codify `PUBLIC`/`anon` revocation and `authenticated` execution for all 13 reviewed RPCs, and add CI-owned regression coverage. Do not rewrite already-correct privileged functions or add client-facing RLS policies merely to silence advisor output.

**Tech Stack:** PostgreSQL / Supabase migrations and ACL metadata, Node.js built-in test runner, GitHub Actions, Supabase advisors.

**Spec:** `docs/superpowers/specs/2026-09-14-supabase-rpc-least-privilege-design.md`

## Global Constraints

- Preserve intentional browser RPC entry points; do not blanket-revoke `authenticated`.
- Do not blanket-convert reviewed RPCs to `SECURITY INVOKER`.
- `PUBLIC` and `anon` must not have `EXECUTE` on any reviewed RPC.
- Do not add `service_role` execution unless a proven server caller needs it.
- Privileged RPCs must continue to reject missing/disabled profiles and unauthorized roles before protected state changes.
- Manager invite restrictions and Admin-only exceptions remain unchanged.
- Guardian approval, isolated-test, merge/deploy, and kill-switch boundaries remain unchanged or stricter.
- The 17 RLS/no-policy tables remain server-only; add no browser table grants or permissive policies.
- Leaked-password protection is a separate Auth setting and is not changed here.
- No pricing, recovery, OAuth, or unrelated schema behavior changes.

---

### Task 1: Add a CI-owned RED contract

**Files:**
- Create: `tests/supabase-rpc-least-privilege-contract.test.mjs`
- Modify: `.github/workflows/quality-gate.yml`

**Interfaces:**
- Consumes: the approved role matrix.
- Produces: a focused contract for migration existence, ACL intent, diagnostic enabled-profile enforcement, and no table-exposure drift.

- [ ] **Step 1: Create the contract test**

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

test('reviewed RPCs explicitly revoke browser-default execution and grant authenticated entry only', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8').toLowerCase();
  for (const name of rpcNames) {
    assert.match(sql, new RegExp(`revoke\\s+execute\\s+on\\s+function\\s+public\\.${name}\\(`));
    assert.match(sql, new RegExp(`grant\\s+execute\\s+on\\s+function\\s+public\\.${name}\\(`));
  }
  assert.match(sql, /from\s+public\s*,\s*anon/i);
  assert.match(sql, /to\s+authenticated/i);
  assert.doesNotMatch(sql, /grant\s+execute[^;]+to\s+service_role/i);
});

test('Guardian diagnostics require an enabled profile', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');
  assert.match(sql, /guardian_report_diagnostic/);
  assert.match(sql, /from\s+public\.profiles\s+p/i);
  assert.match(sql, /p\.id\s*=\s*auth\.uid\(\)/i);
  assert.match(sql, /p\.is_enabled\s*=\s*true/i);
  assert.match(sql, /Authentication required/i);
});

test('migration cannot broaden table access or add privileged Guardian transitions', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');
  assert.doesNotMatch(sql, /create\s+policy/i);
  assert.doesNotMatch(sql, /grant\s+(select|insert|update|delete|all)\s+on\s+(table\s+)?public\./i);
  assert.doesNotMatch(sql, /disable\s+row\s+level\s+security/i);
  assert.doesNotMatch(sql, /security\s+invoker/i);
  assert.doesNotMatch(sql, /insert\s+into\s+public\.guardian_repairs/i);
  assert.doesNotMatch(sql, /update\s+public\.guardian_repairs/i);
});
```

- [ ] **Step 2: Wire it into the existing quality gate**

Add immediately after the existing Morley ecosystem contract step:

```yaml
      - name: Supabase RPC least-privilege contract
        run: node --test tests/supabase-rpc-least-privilege-contract.test.mjs
```

- [ ] **Step 3: Run the focused test**

Run:

```bash
node --test tests/supabase-rpc-least-privilege-contract.test.mjs
```

Expected: FAIL with `missing approved RPC hardening migration`.

- [ ] **Step 4: Commit RED**

```bash
git add tests/supabase-rpc-least-privilege-contract.test.mjs .github/workflows/quality-gate.yml
git commit -m "test: define Supabase RPC least-privilege contract"
```

---

### Task 2: Add the minimal hardening migration

**Files:**
- Create: `supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql`
- Test: `tests/supabase-rpc-least-privilege-contract.test.mjs`

**Interfaces:**
- Consumes: current live definitions and exact reviewed RPC signatures.
- Produces: enabled-profile enforcement for Guardian diagnostics plus explicit ACL normalization for all 13 RPCs.

- [ ] **Step 1: Fail closed on schema/signature drift**

Start the migration with a `DO` block using `to_regprocedure(...)`. Require all exact signatures below to resolve before any ACL mutation:

```text
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

The block must raise an exception naming the missing signature if any lookup returns null.

- [ ] **Step 2: Recreate only `guardian_report_diagnostic` with the stricter opening guard**

Preserve its existing signature, return type, `SECURITY DEFINER`, explicit search path, severity validation, metadata redaction, route/message normalization, server-derived fingerprint, unresolved-incident reuse, terminal-incident deduplication, and incident creation logic. Replace only this guard:

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

Do not add any `guardian_repairs` insert/update, approval transition, merge, or deployment behavior to this function.

- [ ] **Step 3: Normalize ACLs for all 13 exact signatures**

For each signature in Step 1 add both statements using the full argument list:

```sql
revoke execute on function public.<function>(<exact types>) from public, anon;
grant execute on function public.<function>(<exact types>) to authenticated;
```

Do not grant `service_role`; do not change schema/table privileges.

- [ ] **Step 4: Add rollback notes inside the migration**

Record that pre-change RPC ACL posture was `postgres + authenticated`, with no `anon`, `PUBLIC`, or `service_role` execution. Record that rollback of the diagnostic body restores only the previous `auth.uid() is null` guard and must not change RLS, table grants, or Guardian approval protections.

- [ ] **Step 5: Run GREEN tests**

```bash
node --test tests/supabase-rpc-least-privilege-contract.test.mjs
node --test tests/morley-ecosystem-contract.test.mjs
```

Expected: both PASS.

- [ ] **Step 6: Commit**

```bash
git add supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql
git commit -m "security: harden authenticated definer RPC boundary"
```

---

### Task 3: Lock server-only table posture in the contract

**Files:**
- Modify: `tests/supabase-rpc-least-privilege-contract.test.mjs`

**Interfaces:**
- Consumes: Task 2 migration.
- Produces: regression proof that advisor INFO findings are not “fixed” by widening browser access.

- [ ] **Step 1: Append the representative-table test**

```js
test('server-only advisor tables remain outside browser grants', () => {
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

- [ ] **Step 2: Run and commit**

```bash
node --test tests/supabase-rpc-least-privilege-contract.test.mjs
git add tests/supabase-rpc-least-privilege-contract.test.mjs
git commit -m "test: lock server-only Supabase table posture"
```

Expected: PASS before commit.

---

### Task 4: Open the implementation PR and verify exact-head CI

**Files:**
- Review only; no new production changes.

**Interfaces:**
- Consumes: Tasks 1-3.
- Produces: a reviewable implementation PR with exact-head evidence.

- [ ] **Step 1: Start from current `main`**

Create `security/supabase-rpc-least-privilege-20260914` from the latest `main`. If `main` advanced after this plan was written, copy only the approved test/workflow/migration changes rather than merging unrelated design-branch history.

- [ ] **Step 2: Open the PR with #2033 scope clearly stated**

Changed production behavior must be limited to disabled-profile denial in Guardian diagnostics and explicit ACL normalization.

- [ ] **Step 3: Require all exact-head gates GREEN**

Verify success for:

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

Cancelled workflows must be rerun; cancellation is not a pass.

- [ ] **Step 4: Review the full PR diff**

Allow only:

```text
tests/supabase-rpc-least-privilege-contract.test.mjs
.github/workflows/quality-gate.yml
supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql
```

A directly required test-only change is acceptable only if it is documented in the PR. Reject Auth-setting changes, new RLS policies, browser table grants, pricing/recovery changes, or Guardian approval/deployment changes.

- [ ] **Step 5: Merge using the verified head SHA**

Use expected-head protection so a moving PR cannot bypass reviewed CI evidence.

---

### Task 5: Deploy and verify production safely

**Files:**
- Apply exactly: `supabase/migrations/20260914151500_harden_authenticated_security_definer_rpcs.sql`

**Interfaces:**
- Consumes: merged migration.
- Produces: verified live ACL/function state and classified advisor output.

- [ ] **Step 1: Apply via the Supabase migration API**

Use migration name:

```text
harden_authenticated_security_definer_rpcs
```

Do not execute an ad-hoc SQL variant.

- [ ] **Step 2: Verify all 13 live function ACLs**

Query `pg_proc`, `pg_namespace`, and `has_function_privilege`. Expected for each reviewed RPC:

```text
SECURITY DEFINER = true
authenticated EXECUTE = true
anon EXECUTE = false
PUBLIC EXECUTE = false
service_role EXECUTE = false
```

- [ ] **Step 3: Verify the diagnostic body**

Inspect `pg_get_functiondef(public.guardian_report_diagnostic(...))`. Confirm the enabled-profile predicate:

```sql
p.id = auth.uid()
p.is_enabled = true
```

Also confirm the existing metadata redaction and server-derived fingerprint code remains present and no repair/approval mutation was added.

- [ ] **Step 4: Verify representative server-only tables remain closed**

Using `has_table_privilege`, confirm both `anon` and `authenticated` have no SELECT/INSERT/UPDATE/DELETE privileges on:

```text
device_buy_prices
device_buy_price_history
nova_catalog_audit_queue
nova_knowledge_items
nova_knowledge_chunks
user_drive_backup_keys
user_drive_backup_master_keys
```

- [ ] **Step 5: Run safe negative canaries**

Unauthenticated RPC calls must fail. Use an existing non-privileged test fixture or rollback-only transaction for authenticated denial checks; do not alter real staff roles or create persistent production identities. Confirm where safely testable:

```text
Staff cannot perform inventory/Guardian privileged state changes.
Disabled or missing-profile users cannot submit Guardian diagnostics.
Managers cannot disengage an engaged Guardian kill switch.
Manager invite restrictions remain unchanged.
```

If a safe fixture does not exist for one case, use function-definition/ACL verification rather than mutating production identity state.

- [ ] **Step 6: Re-run Supabase security and performance advisors**

Expected classification:

```text
The SECURITY DEFINER warning may remain because authenticated browser execution is intentional.
The 17 RLS/no-policy INFO findings may remain because those tables are intentionally server-only.
Leaked-password protection may remain WARN until changed through an authorized Auth-management surface.
No new security-advisor category may appear because of this migration.
```

- [ ] **Step 7: Record production evidence on #2033**

Include merge SHA, migration version, 13-RPC ACL verification, diagnostic enabled-profile proof, representative table-closure proof, exact-head CI results, and advisor classification.

---

### Task 6: Final verification and issue disposition

**Files:**
- Review migration comments and issue #2033 evidence.

**Interfaces:**
- Consumes: Task 5 production evidence.
- Produces: a truthful completion decision for the RPC-hardening sub-lane.

- [ ] **Step 1: Recheck post-merge `main` workflows**

Do not claim completion while any required post-merge workflow is failed, queued indefinitely, or still running.

- [ ] **Step 2: Confirm rollback readiness**

Rollback must restore only the pre-change diagnostic guard/ACL state. It must never disable RLS, add browser table grants, persist privileged credentials, or weaken Guardian approval boundaries.

- [ ] **Step 3: Classify #2033**

If RPC hardening and server-only table verification are complete but leaked-password protection is still disabled, keep #2033 open or split that remaining Auth setting into a dedicated protected issue before closing #2033. Do not claim the entire security-advisor set is cleared while the Auth warning remains.
