import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadMatcher() {
  const source = fs.readFileSync(new URL('../match-quality.js', import.meta.url), 'utf8');
  const window = { fetch: async () => { throw new Error('network not used in unit tests'); } };
  const document = {
    readyState: 'loading',
    addEventListener() {},
    getElementById() { return null; },
    querySelectorAll() { return []; },
    body: {},
  };
  class MutationObserver { constructor() {} observe() {} }
  const sandbox = {
    window,
    document,
    MutationObserver,
    Response,
    JSON,
    Number,
    String,
    RegExp,
    Set,
    Math,
    Array,
    Object,
    console,
    setTimeout,
    clearTimeout,
  };
  vm.runInNewContext(source, sandbox, { filename: 'match-quality.js' });
  return window.MorleyMatchQuality;
}

const matcher = loadMatcher();

test('exports variant-aware laptop matcher', () => {
  assert.equal(matcher?.version, 2);
  assert.equal(typeof matcher?.score, 'function');
});

test('accepts an exact laptop configuration', () => {
  const target = 'Lenovo ThinkPad T14 Gen 5 Intel Core Ultra 7 155U 16GB RAM 512GB SSD';
  const listing = 'Lenovo ThinkPad T14 Gen 5 Laptop Core Ultra 7 155U 16GB RAM 512GB SSD';
  const result = matcher.score(target, listing);
  assert.equal(result.hardReject, false);
  assert.ok(result.score >= 55, `expected eligible score, got ${result.score}`);
});

test('rejects an explicit RAM mismatch', () => {
  const target = 'Lenovo ThinkPad T14 Gen 5 Intel Core Ultra 7 155U 16GB RAM 512GB SSD';
  const listing = 'Lenovo ThinkPad T14 Gen 5 Laptop Core Ultra 7 155U 32GB RAM 512GB SSD';
  const result = matcher.score(target, listing);
  assert.equal(result.hardReject, true);
  assert.ok(result.warnings.includes('different RAM'));
});

test('rejects an explicit storage mismatch', () => {
  const target = 'Dell XPS 13 9310 Intel Core i7-1165G7 16GB RAM 512GB SSD';
  const listing = 'Dell XPS 13 9310 Laptop Intel Core i7-1165G7 16GB RAM 1TB SSD';
  const result = matcher.score(target, listing);
  assert.equal(result.hardReject, true);
  assert.ok(result.warnings.includes('different storage'));
});

test('rejects a different processor platform', () => {
  const target = 'ASUS Zephyrus G16 2025 Intel Core Ultra 9 285H 32GB RAM 1TB SSD';
  const listing = 'ASUS Zephyrus G16 2025 AMD Ryzen AI 9 HX 370 32GB RAM 1TB SSD Laptop';
  const result = matcher.score(target, listing);
  assert.equal(result.hardReject, true);
  assert.ok(result.warnings.includes('different CPU platform') || result.warnings.includes('different CPU'));
});

test('rejects accessories and parts', () => {
  const target = 'Apple MacBook Air M3 16GB RAM 512GB SSD';
  const listing = '100W USB-C Charger Adapter for Apple MacBook Air M3';
  const result = matcher.score(target, listing);
  assert.equal(result.hardReject, true);
  assert.equal(result.label, 'Rejected');
});

test('does not invent a mismatch when listing omits RAM or storage', () => {
  const target = 'Apple MacBook Air M3 16GB RAM 512GB SSD';
  const listing = 'Apple MacBook Air M3 13-inch Laptop';
  const result = matcher.score(target, listing);
  assert.equal(result.hardReject, false);
  assert.ok(!result.warnings.includes('different RAM'));
  assert.ok(!result.warnings.includes('different storage'));
});
