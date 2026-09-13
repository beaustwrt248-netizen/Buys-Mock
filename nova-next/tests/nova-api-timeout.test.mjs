import assert from 'node:assert/strict';
import { createNovaApi } from '../src/adapters/nova-api.mjs';

let capturedSignal = null;
const api = createNovaApi({
  getAccessToken: () => 'token',
  requestTimeoutMs: 15,
  transport: ({ signal }) => new Promise(resolve => {
    capturedSignal = signal;
    setTimeout(() => resolve({ ok: true, data: { late: true } }), 80);
  })
});

const started = Date.now();
const result = await api.run('catalogue-audit-read');
const elapsed = Date.now() - started;

assert.equal(result.ok, false);
assert.equal(result.error.code, 'REQUEST_TIMEOUT');
assert.equal(capturedSignal?.aborted, true);
assert.ok(elapsed < 60, `timeout should return promptly, got ${elapsed}ms`);
console.log('nova-api-timeout: ok');
