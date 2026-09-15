import assert from 'node:assert/strict';
import { createAuthController } from '../src/auth-controller.mjs';

function store(initial = null) {
  let value = initial;
  return { load: () => value, save: next => (value = next), clear: () => { value = null; }, peek: () => value };
}

const stored = { access_token: 'existing', refresh_token: 'refresh', user: { id: 'u1', email: 'admin@example.com' } };
const sessionStore = store(stored);
const controller = createAuthController({
  authClient: {
    validateAdminProfile: async () => { const error = new Error('AUTH_TIMEOUT'); error.code = 'AUTH_TIMEOUT'; throw error; },
    refreshSession: async () => { throw new Error('refresh must not run when validation is merely unavailable'); },
    signInPassword: async () => stored
  },
  sessionStore
});

const result = await controller.restore();
assert.equal(result.status, 'degraded', 'transient validation failure must not be treated as an invalid session');
assert.equal(result.error, 'AUTH_VALIDATION_UNAVAILABLE');
assert.equal(sessionStore.peek()?.access_token, 'existing', 'stored credentials must remain intact on transient validation failure');
assert.equal(controller.getAccessToken(), '', 'unvalidated credentials must not be exposed to protected services while degraded');

console.log('session-transient-validation-regression: ok');
