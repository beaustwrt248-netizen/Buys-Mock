import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-aspera-chat-model-evidence.json', import.meta.url), 'utf8'));

test('Aspera Chat evidence remains non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
});

test('Aspera Chat records manufacturer model H6031 with zero dependencies', () => {
  assert.equal(evidence.verified_rows.length, 1);
  const row = evidence.verified_rows[0];
  assert.equal(row.device_catalog_id, 91);
  assert.equal(row.verified_model_number, 'H6031');
  assert.deepEqual(row.dependency_snapshot, { inventory_refs: 0, pricing_refs: 0, pricing_history_refs: 0 });
  assert.equal(row.collision_snapshot.active_rows_using_verified_model_number, 0);
});

test('evidence stays anchored to Aspera manufacturer resources', () => {
  const row = evidence.verified_rows[0];
  assert.ok(row.evidence.every((entry) => entry.authority === 'Aspera Mobile'));
  assert.ok(row.evidence.some((entry) => entry.url.endsWith('/Chat-user-manual.pdf')));
  assert.ok(evidence.safety_notes.some((note) => note.includes('SC9863T')));
});
