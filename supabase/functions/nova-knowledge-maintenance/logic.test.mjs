import test from 'node:test';
import assert from 'node:assert/strict';
import { classifySourceFailure, summarizeSourceFailures } from './logic.mjs';

test('gateway timeout is a retryable source-boundary failure', () => {
  assert.deepEqual(classifySourceFailure({ message: 'Gateway Timeout' }), {
    retryable: true,
    kind: 'transient_gateway',
  });
  assert.deepEqual(classifySourceFailure({ message: 'upstream returned 503 Service Unavailable' }), {
    retryable: true,
    kind: 'transient_gateway',
  });
});

test('permission, auth and schema failures stay fail-closed', () => {
  assert.deepEqual(classifySourceFailure({ code: '42501', message: 'permission denied for table x' }), {
    retryable: false,
    kind: 'permission',
  });
  assert.deepEqual(classifySourceFailure({ code: '28P01', message: 'password authentication failed' }), {
    retryable: false,
    kind: 'auth',
  });
  assert.deepEqual(classifySourceFailure({ code: '42P01', message: 'relation does not exist' }), {
    retryable: false,
    kind: 'schema',
  });
});

test('unknown failures remain hard failures', () => {
  assert.deepEqual(classifySourceFailure({ message: 'unexpected response shape' }), {
    retryable: false,
    kind: 'hard',
  });
});

test('source failure summaries include only source adapter and failure kind', () => {
  const summary = summarizeSourceFailures([
    { source: 'device_catalog', kind: 'transient_gateway', raw: 'token=do-not-leak' },
    { source: 'support_tickets', kind: 'transient_gateway', raw: 'password=do-not-leak' },
  ]);
  assert.equal(summary, 'source=device_catalog:transient_gateway; source=support_tickets:transient_gateway');
  assert.equal(summary.includes('do-not-leak'), false);
});
