import test from 'node:test';
import assert from 'node:assert/strict';
import {
  boundedBatch,
  catalogAuditRunPatch,
  classifySourceEvidence,
  isSafePublicSourceUrl,
  normalizePageText,
  resolveSafeRedirect,
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

test('source evidence verifies complete rows despite harmless URL-style punctuation', () => {
  const device = { brand: 'Google', model_name: 'Pixel 10 Pro', model_number: 'GP4BC', release_year: 2025, key_specs: { chipset: 'Tensor G5' } };
  const result = classifySourceEvidence(device, '<html><body>Google Pixel-10-Pro (GP4BC) specifications</body></html>');
  assert.deepEqual(result, { outcome: 'verified', code: 'source_identity_confirmed', field: null });
});

test('catalogue display-year suffix does not hide a missing model-number finding', () => {
  const device = { brand: 'GPD', model_name: 'GPD WIN Mini (2024)', model_number: null, release_year: 2024, key_specs: { cpu: 'Ryzen 7 8840U' } };
  const result = classifySourceEvidence(device, 'GPD WIN Mini Tech Specs - Shenzhen GPD Technology Co., Ltd.');
  assert.equal(result.outcome, 'blocked');
  assert.equal(result.code, 'missing_model_number_requires_review');
  assert.equal(result.field, 'model_number');
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

test('source fetch rejects loopback, private, link-local and credential-bearing URLs', () => {
  assert.equal(isSafePublicSourceUrl('https://www.samsung.com/au/phones/'), true);
  assert.equal(isSafePublicSourceUrl('http://127.0.0.1/admin'), false);
  assert.equal(isSafePublicSourceUrl('http://169.254.169.254/latest/meta-data'), false);
  assert.equal(isSafePublicSourceUrl('http://192.168.1.1/'), false);
  assert.equal(isSafePublicSourceUrl('http://user:pass@example.com/'), false);
  assert.equal(isSafePublicSourceUrl('http://[::1]/'), false);
});

test('redirect targets are resolved and revalidated before following', () => {
  assert.equal(resolveSafeRedirect('https://example.com/a', '/b'), 'https://example.com/b');
  assert.equal(resolveSafeRedirect('https://example.com/a', 'http://169.254.169.254/latest/meta-data'), null);
  assert.equal(resolveSafeRedirect('https://example.com/a', 'http://user:pass@example.com/private'), null);
});

test('normalizer removes scripts and markup', () => {
  assert.equal(normalizePageText('<style>x{}</style><script>secret()</script><p>Hello &amp; world</p>'), 'hello & world');
});

test('diagnostics redact common secret material', () => {
  assert.equal(sanitizeError('token=abc123 timeout'), 'token=[redacted] timeout');
});

test('source tier is manufacturer-first and conservative for unknown sources', () => {
  assert.equal(sourceTier('https://www.samsung.com/au/support/', 'Samsung Support', 'Samsung'), 1);
  assert.equal(sourceTier('https://www.telstra.com.au/mobile-phones', 'Telstra', 'Samsung'), 3);
  assert.equal(sourceTier('https://example.com/device', 'Unknown', 'Samsung'), 5);
  assert.equal(sourceTier('not-a-url', 'Unknown', 'Samsung'), 5);
});

test('short brand names do not match arbitrary hostname substrings', () => {
  assert.equal(sourceTier('https://catalogue.example.com/lg-device', 'Independent catalogue', 'LG'), 5);
});

test('terminal status excludes pending/in-progress', () => {
  assert.equal(terminalStatus('verified'), true);
  assert.equal(terminalStatus('blocked'), true);
  assert.equal(terminalStatus('in_progress'), false);
});

test('terminal catalogue run patch finalizes a run even when no current batch row references it', () => {
  const patch = catalogAuditRunPatch([
    ...Array.from({ length: 62 }, () => ({ status: 'blocked' })),
    ...Array.from({ length: 13 }, () => ({ status: 'verified' })),
  ], '2026-09-14T15:00:00.000Z');
  assert.deepEqual(patch, {
    status: 'completed',
    scanned_count: 75,
    verified_count: 13,
    discrepancy_count: 62,
    error_count: 0,
    notes: 'verified=13; blocked=62; discrepancy=0; failed=0; pending=0; in_progress=0',
    finished_at: '2026-09-14T15:00:00.000Z',
  });
});

test('non-terminal catalogue run patch remains running and does not set finished_at', () => {
  const patch = catalogAuditRunPatch([
    ...Array.from({ length: 29 }, () => ({ status: 'blocked' })),
    ...Array.from({ length: 46 }, () => ({ status: 'pending' })),
  ], '2026-09-14T15:00:00.000Z');
  assert.equal(patch.status, 'running');
  assert.equal(patch.scanned_count, 75);
  assert.equal(patch.discrepancy_count, 29);
  assert.equal('finished_at' in patch, false);
});
