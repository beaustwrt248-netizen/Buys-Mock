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

function baseQuote(overrides = {}) {
  return {
    identityResolved: true,
    storageResolved: true,
    evidenceSufficient: true,
    evidenceConfidence: 0.9,
    marketResale: 600,
    targetMargin: 150,
    minMargin: 100,
    ...overrides,
  };
}

test('applies a verified storage adjustment when the market baseline is not storage-specific', () => {
  const core = loadCore();
  const result = core.buildValuationQuote(baseQuote({
    storage: '256 GB',
    storageAdjustment: 35,
    marketBaselineStorageSpecific: false,
  }));

  assert.equal(result.targetResale, 635);
  assert.equal(result.inputs.storage, '256 GB');
  assert.equal(result.inputs.storageAdjustment, 35);
  assert.equal(result.inputs.marketBaselineStorageSpecific, false);
  assert.ok(result.explanation.some(line => /Storage adjustment: \$35\.00/.test(line)));
});

test('does not double-count storage when verified market comps already match the exact storage variant', () => {
  const core = loadCore();
  const result = core.buildValuationQuote(baseQuote({
    storage: '256 GB',
    storageAdjustment: 35,
    marketBaselineStorageSpecific: true,
  }));

  assert.equal(result.targetResale, 600);
  assert.equal(result.inputs.storageAdjustment, 0);
  assert.equal(result.inputs.marketBaselineStorageSpecific, true);
  assert.ok(result.explanation.some(line => /already storage-specific/i.test(line)));
});

test('unknown battery health is valuation-neutral and is not treated as a healthy battery', () => {
  const core = loadCore();
  const result = core.buildValuationQuote(baseQuote({ batteryHealthPct: null }));

  assert.equal(result.targetResale, 600);
  assert.equal(result.inputs.batteryHealthPct, null);
  assert.equal(result.inputs.batteryHealthBand, 'unknown');
  assert.equal(result.inputs.batteryHealthAdjustment, 0);
  assert.ok(result.explanation.some(line => /Battery health: unknown; no value adjustment applied/i.test(line)));
});

test('battery health below the replacement threshold reduces suggested pricing with an explainable adjustment', () => {
  const core = loadCore();
  const result = core.buildValuationQuote(baseQuote({
    batteryHealthPct: 78,
    batteryReplacementCost: 45,
  }));

  assert.equal(result.inputs.batteryHealthPct, 78);
  assert.equal(result.inputs.batteryHealthBand, 'replacement_risk');
  assert.equal(result.inputs.batteryHealthAdjustment, -60);
  assert.equal(result.targetResale, 540);
  assert.ok(result.explanation.some(line => /Battery health 78%.*-\$60\.00/i.test(line)));
});

test('excellent battery health is neutral and cannot create a valuation premium', () => {
  const core = loadCore();
  const result = core.buildValuationQuote(baseQuote({
    batteryHealthPct: 95,
    batteryHealthAdjustment: 40,
  }));

  assert.equal(result.inputs.batteryHealthBand, 'excellent');
  assert.equal(result.inputs.batteryHealthAdjustment, 0);
  assert.equal(result.targetResale, 600);
});
