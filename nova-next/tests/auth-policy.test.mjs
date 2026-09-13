import assert from 'node:assert/strict';
import { canEnterNovaNext } from '../src/auth-policy.mjs';

assert.equal(canEnterNovaNext({ role: 'admin', is_enabled: true }), true);
assert.equal(canEnterNovaNext({ role: 'admin', is_enabled: false }), false);
assert.equal(canEnterNovaNext({ role: 'manager', is_enabled: true }), false);
assert.equal(canEnterNovaNext({ role: 'staff', is_enabled: true }), false);
assert.equal(canEnterNovaNext(null), false);
console.log('auth-policy: ok');
