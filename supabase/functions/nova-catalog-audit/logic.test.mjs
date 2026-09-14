import test from 'node:test';
import assert from 'node:assert/strict';
import {
  boundedBatch,
  classifySourceEvidence,
  normalizePageText,
  retryDelaySeconds,
  sanitizeError,
  sourceTier,
  terminalStatus,
} from './logic.mjs';

test('batch bounds cannot exceed worker safety limit', () => {
  assert.equal(boundedBatch(0, 10, 25), 10);
  assert.equal(boundedBatch(9.9, 10, 25), 9);
  assert.equal(boundedBatch(500, 10, 25), 25);
});

test('retry delay backs off exponentially and caps', () => {
  assert.equal(retryDelaySeconds(1), 300);
  assert.equal(retryDelaySeconds(2), 600);
  assert.equal(retryDelaySeconds(20), 21600);
});

test('source evidence verifies only complete catalogue rows with matching identity', () => {
  const device = { brand: 'Google', model_name: 'Pixel 10 Pro', model_number: 'GP4BC', release_year: 2025, key_specs: { chipset: 'Tensor G5' } };
  const result = classifySourceEvidence(device, '<html><body>Google Pixel 10 Pro GP4BC specifications</body></html>');
  assert.deepEqual(result, { outcome: 'verified', code: 'source_identity_confirmed', field: null });
});

test('missing facts never become invented verified facts', () => {
  const device = { brand: 'Google', model_name: 'Pixel 10 Pro', model_number: null, release_year: 2025, key_specs: { chipset: 'Tensor G5' } };
  const result = classifySourceEvidence(device, 'Google Pixel 10 Pro specifications');
  assert.equal(result.outcome, 'blocked');
  assert.equal(result.field, 'model_number');
});

test('identity mismatch blocks instead of overwriting catalogue data', () => {
  const device = { brand: 'Samsung', model_name: 'Galaxy S26', model_number: 'SM-S942B', release_year: 2026, key_specs: { storage: '256GB' } };
  assert.equal(classifySourceEvidence(device, 'Unrelated product page').outcome, 'blocked');
});

test('normalizer removes scripts and markup', () => {
  assert.equal(normalizePageText('<style>x{}</style><script>secret()</script><p>Hello &amp; world</p>'), 'hello & world');
});

test('diagnostics redact common secret material', () => {
  assert.equal(sanitizeError('token=abc123 timeout'), 'token=[redacted] timeout');
});

test('source tier is conservative and source based', () => {
  assert.equal(sourceTier('https://www.telstra.com.au/mobile-phones', 'Telstra'), 3);
  assert.equal(sourceTier('not-a-url', 'Unknown'), 5);
});

test('terminal status excludes pending/in-progress', () => {
  assert.equal(terminalStatus('verified'), true);
  assert.equal(terminalStatus('blocked'), true);
  assert.equal(terminalStatus('in_progress'), false);
});
