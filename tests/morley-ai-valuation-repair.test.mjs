import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadCore() {
  const source = fs.readFileSync(new URL('../morley-ai-assessment-core.js', import.meta.url), 'utf8');
  const window = {};
  const context = vm.createContext({ window, globalThis: window, Object, Array, Number, String, Boolean, Math, Set, Map, JSON });
  vm.runInContext(source, context, { filename: 'morley-ai-assessment-core.js' });
  return window.MorleyAssessmentCore;
}

test('valuation is blocked when verified evidence is incomplete', () => {
  const core = loadCore();
  const result = core.buildValuationQuote({
    identityResolved: true,
    storageResolved: false,
    evidenceSufficient: true,
    marketResale: 500,
    targetMargin: 140,
  });
  assert.equal(result.state, 'blocked');
  assert.equal(result.buyOffer, null);
  assert.ok(result.blockers.includes('storage_unresolved'));
});

test('builds an explainable advisory buy quote with explicit adjustments and hard caps', () => {
  const core = loadCore();
  const result = core.buildValuationQuote({
    identityResolved: true,
    storageResolved: true,
    evidenceSufficient: true,
    marketResale: 600,
    conditionAdjustment: -40,
    stockAdjustment: -20,
    demandAdjustment: 30,
    repairCost: 50,
    targetMargin: 150,
    maxBuy: 390,
    minMargin: 120,
    evidenceConfidence: 0.9,
  });
  assert.equal(result.state, 'proposed');
  assert.equal(result.targetResale, 570);
  assert.equal(result.buyOffer, 370);
  assert.equal(result.expectedMargin, 150);
  assert.equal(result.commercialAuthority, 'advisory');
  assert.equal(result.requiresStaffConfirmation, true);
  assert.equal(result.inputs.conditionAdjustment, -40);
  assert.equal(result.inputs.repairCost, 50);
  assert.ok(result.explanation.length >= 4);
});

test('hard max-buy rule wins over AI recommendation', () => {
  const core = loadCore();
  const result = core.buildValuationQuote({
    identityResolved: true,
    storageResolved: true,
    evidenceSufficient: true,
    marketResale: 900,
    targetMargin: 100,
    maxBuy: 450,
    minMargin: 100,
  });
  assert.equal(result.buyOffer, 450);
  assert.equal(result.constraints.maxBuyApplied, true);
  assert.ok(result.expectedMargin >= 100);
});

test('quote refuses to propose a buy when hard minimum margin cannot be met', () => {
  const core = loadCore();
  const result = core.buildValuationQuote({
    identityResolved: true,
    storageResolved: true,
    evidenceSufficient: true,
    marketResale: 120,
    repairCost: 80,
    targetMargin: 60,
    minMargin: 60,
    maxBuy: 100,
  });
  assert.equal(result.state, 'review_required');
  assert.equal(result.buyOffer, null);
  assert.ok(result.blockers.includes('minimum_margin_unachievable'));
});

test('repair engine compares as-is and repaired economics and remains advisory', () => {
  const core = loadCore();
  const result = core.decideRepairStrategy({
    buyCost: 250,
    resaleAsIs: 330,
    resaleAfterRepair: 520,
    repairCost: 70,
    minMargin: 120,
    repairDays: 3,
    sellThroughDaysAsIs: 7,
    sellThroughDaysAfterRepair: 10,
  });
  assert.equal(result.recommendation, 'buy_and_repair');
  assert.equal(result.repairedMargin, 200);
  assert.equal(result.asIsMargin, 80);
  assert.equal(result.commercialAuthority, 'advisory');
  assert.equal(result.requiresStaffConfirmation, true);
});

test('repair engine flags low-margin economics for staff review instead of auto rejecting', () => {
  const core = loadCore();
  const result = core.decideRepairStrategy({
    buyCost: 300,
    resaleAsIs: 350,
    resaleAfterRepair: 410,
    repairCost: 80,
    minMargin: 100,
  });
  assert.equal(result.recommendation, 'review_required');
  assert.match(result.reason, /margin/i);
  assert.notEqual(result.recommendation, 'auto_reject');
});

test('repair engine recommends parts-only when verified parts recovery is the strongest route clearing policy', () => {
  const core = loadCore();
  const result = core.decideRepairStrategy({
    commercialInputsVerified: true,
    buyCost: 100,
    resaleAsIs: 150,
    resaleAfterRepair: 220,
    repairCost: 90,
    partsRecoveryValue: 260,
    partsProcessingCost: 20,
    minMargin: 100,
  });
  assert.equal(result.recommendation, 'parts_only');
  assert.equal(result.partsMargin, 140);
  assert.match(result.reason, /parts/i);
});

test('repair engine tie handling is deterministic and avoids unnecessary work', () => {
  const core = loadCore();
  const result = core.decideRepairStrategy({
    commercialInputsVerified: true,
    buyCost: 100,
    resaleAsIs: 250,
    resaleAfterRepair: 300,
    repairCost: 50,
    partsRecoveryValue: 250,
    minMargin: 100,
  });
  assert.equal(result.asIsMargin, 150);
  assert.equal(result.repairedMargin, 150);
  assert.equal(result.partsMargin, 150);
  assert.equal(result.recommendation, 'buy_as_is');
});

test('repair engine requires staff review when required commercial inputs are missing or unverified', () => {
  const core = loadCore();
  const missing = core.decideRepairStrategy({
    commercialInputsVerified: true,
    buyCost: 100,
    resaleAsIs: 250,
    repairCost: 50,
    minMargin: 100,
  });
  assert.equal(missing.recommendation, 'review_required');
  assert.ok(missing.blockers.includes('commercial_inputs_incomplete'));

  const unverified = core.decideRepairStrategy({
    commercialInputsVerified: false,
    buyCost: 100,
    resaleAsIs: 250,
    resaleAfterRepair: 300,
    repairCost: 50,
    minMargin: 100,
  });
  assert.equal(unverified.recommendation, 'review_required');
  assert.ok(unverified.blockers.includes('commercial_inputs_unverified'));
});
