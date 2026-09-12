import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadCore() {
  const source = fs.readFileSync(new URL('../morley-ai-assessment-core.js', import.meta.url), 'utf8');
  const window = {};
  vm.runInNewContext(source, { window, globalThis: window, Object, Array, Number, String, Boolean, Math, Set, Map, JSON }, { filename: 'morley-ai-assessment-core.js' });
  return window.MorleyAssessmentCore;
}

test('recommends buy and repair when repaired margin is strongest and clears policy', () => {
  const result = loadCore().decideRepairStrategy({ buyCost: 200, resaleAsIs: 300, resaleAfterRepair: 500, repairCost: 80, partsRecoveryValue: 230, minMargin: 100 });
  assert.equal(result.recommendation, 'buy_and_repair');
  assert.equal(result.repairedMargin, 220);
  assert.equal(result.requiresStaffConfirmation, true);
});

test('recommends buy as-is when it clears policy and is not beaten by repair or parts', () => {
  const result = loadCore().decideRepairStrategy({ buyCost: 200, resaleAsIs: 360, resaleAfterRepair: 400, repairCost: 80, partsRecoveryValue: 250, minMargin: 100 });
  assert.equal(result.recommendation, 'buy_as_is');
  assert.equal(result.asIsMargin, 160);
});

test('recommends parts-only when parts recovery is the only route clearing margin', () => {
  const result = loadCore().decideRepairStrategy({ buyCost: 100, resaleAsIs: 150, resaleAfterRepair: 220, repairCost: 90, partsRecoveryValue: 260, partsProcessingCost: 20, minMargin: 100 });
  assert.equal(result.recommendation, 'parts_only');
  assert.equal(result.partsMargin, 140);
  assert.match(result.reason, /parts/i);
});

test('returns review required when no route clears the minimum margin', () => {
  const result = loadCore().decideRepairStrategy({ buyCost: 250, resaleAsIs: 300, resaleAfterRepair: 360, repairCost: 80, partsRecoveryValue: 280, partsProcessingCost: 20, minMargin: 100 });
  assert.equal(result.recommendation, 'review_required');
});

test('tie handling is deterministic and prefers selling as-is over unnecessary work', () => {
  const result = loadCore().decideRepairStrategy({ buyCost: 100, resaleAsIs: 250, resaleAfterRepair: 300, repairCost: 50, partsRecoveryValue: 250, minMargin: 100 });
  assert.equal(result.asIsMargin, 150);
  assert.equal(result.repairedMargin, 150);
  assert.equal(result.partsMargin, 150);
  assert.equal(result.recommendation, 'buy_as_is');
});
