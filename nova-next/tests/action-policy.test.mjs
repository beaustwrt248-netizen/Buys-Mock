import assert from 'node:assert/strict';
import { classifyAction, mayAutoExecute } from '../src/action-policy.mjs';

for (const action of ['pricing-write', 'guardian-repair-approve', 'deploy', 'ota-publish', 'role-change', 'destructive-delete', 'signing-change']) {
  assert.equal(mayAutoExecute(action), false, action);
  assert.equal(classifyAction(action).protected, true, action);
}
assert.equal(mayAutoExecute('catalogue-audit-read'), true);
assert.equal(classifyAction('catalogue-audit-read').risk, 'low');
assert.equal(mayAutoExecute('unknown-action'), false);
assert.equal(classifyAction('unknown-action').risk, 'high');
console.log('action-policy: ok');
