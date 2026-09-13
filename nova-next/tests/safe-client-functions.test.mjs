import assert from 'node:assert/strict';
import { SAFE_CLIENT_FUNCTIONS, isSafeClientFunction } from '../src/safe-client-functions.mjs';

for (const name of ['nova-orchestrator','nova-knowledge','nova-learning','nova-attention-control','nova-ai-metrics','nova-vision','nova-code-proposal','nova-github']) {
  assert.equal(isSafeClientFunction(name), true, name);
}
for (const name of ['nova-guardian-intelligence','guardian-repair-executor','guardian-repair-worker','guardian-worker','guardian-repair-status','admin-pricing-control','admin-user-control']) {
  assert.equal(isSafeClientFunction(name), false, name);
  assert.equal(SAFE_CLIENT_FUNCTIONS.includes(name), false, name);
}
console.log('safe-client-functions: ok');
