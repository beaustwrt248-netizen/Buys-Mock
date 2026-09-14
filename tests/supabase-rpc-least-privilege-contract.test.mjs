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

test('migration does not broaden browser table access or let diagnostics mutate approval state', () => {
  const sql = fs.readFileSync(migrationPath, 'utf8');
  assert.doesNotMatch(sql, /create\s+policy/i);
  assert.doesNotMatch(sql, /grant\s+(select|insert|update|delete|all)\s+on\s+(table\s+)?public\./i);
  assert.doesNotMatch(sql, /disable\s+row\s+level\s+security/i);
  assert.doesNotMatch(sql, /security\s+invoker/i);
  assert.doesNotMatch(sql, /update\s+public\.guardian_repairs|state\s*=\s*'applying'|status\s*=\s*'testing'/i);
});

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
