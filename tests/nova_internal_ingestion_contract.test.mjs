import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const sharedPath = 'supabase/functions/nova-knowledge/internal_ingestion.mjs';
const manualPath = 'supabase/functions/nova-knowledge-ingest/index.ts';
const maintenancePath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const adaptersPath = 'supabase/functions/nova-knowledge/internal_adapters.mjs';
const read = (path) => fs.readFileSync(path, 'utf8');

test('manual and scheduled ingestion share one persistence engine', () => {
  assert.equal(fs.existsSync(sharedPath), true, `${sharedPath} must exist`);
  const shared = read(sharedPath);
  const manual = read(manualPath);
  const maintenance = read(maintenancePath);

  assert.match(shared, /export function createInternalIngestionEngine/);
  assert.match(manual, /createInternalIngestionEngine/);
  assert.match(maintenance, /createInternalIngestionEngine/);
  assert.match(manual, /actorId:\s*authorized\.user\.id/);
  assert.match(maintenance, /actorId:\s*null/);

  for (const duplicate of ['hasLegacyCatalogueCoverage', 'upsertSourceAndChunks', 'persistDocument']) {
    assert.doesNotMatch(manual, new RegExp(`(?:async\\s+)?function\\s+${duplicate}\\s*\\(`));
    assert.doesNotMatch(maintenance, new RegExp(`(?:async\\s+)?function\\s+${duplicate}\\s*\\(`));
    assert.match(shared, new RegExp(`(?:async\\s+)?function\\s+${duplicate}\\s*\\(`));
  }
});

test('shared engine preserves internal identity, revision and chunk lifecycle', () => {
  assert.equal(fs.existsSync(sharedPath), true, `${sharedPath} must exist`);
  const shared = read(sharedPath);
  assert.match(shared, /managed_by:\s*["']nova_internal_adapter["']/);
  assert.match(shared, /`internal:\$\{adapterKey\}`/);
  assert.match(shared, /generated_from_live_catalogue/);
  assert.match(shared, /nova_knowledge_revisions/);
  assert.match(shared, /status:\s*["']superseded["']/);
  assert.match(shared, /embedding_status:\s*["']pending["']/);
  assert.match(shared, /return\s+["']created["']/);
  assert.match(shared, /return\s+["']updated["']/);
  assert.match(shared, /return\s+["']skipped["']/);
  assert.match(shared, /return\s+["']adopted["']/);
});

test('endpoint-specific auth, privacy and source policy stay outside the shared engine', () => {
  assert.equal(fs.existsSync(sharedPath), true, `${sharedPath} must exist`);
  const shared = read(sharedPath);
  const manual = read(manualPath);
  const maintenance = read(maintenancePath);
  const adapters = read(adaptersPath);

  assert.match(manual, /p\.role\s*!==\s*["']admin["']/);
  assert.match(manual, /body\.adapter/);
  assert.match(manual, /body\.offset/);
  assert.match(maintenance, /MORLEY_BACKUP_SECRET/);
  assert.match(maintenance, /MAX_EMBEDDING_BATCH\s*=\s*4/);
  assert.match(maintenance, /MAX_INGEST_BATCH\s*=\s*4/);
  assert.match(maintenance, /optional:\s*true/);
  assert.match(maintenance, /normalizeMaintenanceError/);
  assert.match(adapters, /privacy_mode:\s*["']structured_pattern_only["']/);

  assert.doesNotMatch(shared, /MORLEY_BACKUP_SECRET/);
  assert.doesNotMatch(shared, /x-maintenance-secret/i);
  assert.doesNotMatch(shared, /profiles/);
  assert.doesNotMatch(shared, /support_tickets/);
  assert.doesNotMatch(shared, /inventory_items/);
  assert.doesNotMatch(shared, /sales_records/);
});
