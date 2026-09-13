import test from 'node:test';
import assert from 'node:assert/strict';
import { normaliseAsyncState, stateCopy } from '../src/presentation-state.mjs';

test('normaliseAsyncState preserves supported states and freezes output', () => {
  for (const status of ['loading', 'empty', 'unavailable', 'degraded', 'offline', 'protected']) {
    const result = normaliseAsyncState({ status, message: `${status} message`, retryable: true });
    assert.equal(result.kind, status);
    assert.equal(result.message, `${status} message`);
    assert.equal(result.retryable, status === 'protected' ? false : true);
    assert.equal(Object.isFrozen(result), true);
  }
});

test('normaliseAsyncState uses truthful defaults and rejects unknown states', () => {
  assert.deepEqual(normaliseAsyncState({ status: 'mystery' }), {
    kind: 'unavailable',
    message: stateCopy('unavailable'),
    retryable: false
  });
  assert.equal(stateCopy('loading'), 'Loading…');
  assert.equal(stateCopy('empty'), 'Nothing to show yet.');
  assert.equal(stateCopy('degraded'), 'Some information is unavailable.');
  assert.equal(stateCopy('offline'), 'You appear to be offline.');
  assert.equal(stateCopy('protected'), 'This capability is protected.');
});

test('protected state can never be marked retryable', () => {
  assert.equal(normaliseAsyncState({ status: 'protected', retryable: true }).retryable, false);
});
