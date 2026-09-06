import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-nintendo-game-boy-micro-model-evidence.json', import.meta.url)));

test('Game Boy Micro evidence records the console hardware identifier', () => {
  assert.equal(evidence.records.length, 1);
  assert.equal(evidence.records[0].device_catalog_id, 1367);
  assert.equal(evidence.records[0].brand, 'Nintendo');
  assert.equal(evidence.records[0].model_name, 'Game Boy Micro');
  assert.equal(evidence.records[0].verified_model_number, 'OXY-001');
});

test('Game Boy Micro evidence keeps accessory codes separate and remains human gated', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.match(evidence.records[0].evidence.join(' '), /OXY-002/);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /Do not substitute OXY-002/i.test(note)));
});
