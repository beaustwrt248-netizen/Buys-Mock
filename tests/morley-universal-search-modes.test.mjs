import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadModes() {
  const source = fs.readFileSync(new URL('../morley-universal-search-modes.js', import.meta.url), 'utf8');
  const window = {};
  vm.runInNewContext(source, { window, globalThis: window, Object, Array, String, Set }, { filename: 'morley-universal-search-modes.js' });
  return window.MorleyUniversalSearchModes;
}

test('exposes four explicit Morley search modes', () => {
  const modes = loadModes();
  assert.deepEqual([...modes.values], ['quick_search','manual_search','price_check','ai_scan']);
});

test('unknown modes fail closed to quick search', () => {
  const modes = loadModes();
  assert.equal(modes.normalize('manual_search'), 'manual_search');
  assert.equal(modes.normalize('nonsense'), 'quick_search');
  assert.equal(modes.normalize(''), 'quick_search');
});

test('entry points have distinct orchestration without forking catalogue or pricing authority', () => {
  const modes = loadModes();
  const quick = modes.describe('quick_search');
  const manual = modes.describe('manual_search');
  const price = modes.describe('price_check');
  const ai = modes.describe('ai_scan');
  assert.equal(quick.flow, 'instant_lookup');
  assert.equal(manual.flow, 'guided_lookup');
  assert.equal(price.flow, 'valuation_shortcut');
  assert.equal(ai.flow, 'device_lens');
  for (const mode of [quick, manual, price, ai]) {
    assert.equal(mode.catalogueAuthority, 'shared');
    assert.equal(mode.pricingAuthority, 'protected_shared');
  }
});

test('AI scan remains camera-first while non-camera modes use Universal Buy Search', () => {
  const modes = loadModes();
  assert.equal(modes.describe('ai_scan').route, 'device_lens');
  for (const name of ['quick_search','manual_search','price_check']) {
    assert.equal(modes.describe(name).route, 'universal_buy_search');
  }
});
