import assert from 'node:assert/strict';
import { createAuthAdapter } from '../src/adapters/auth-adapter.mjs';

const calls = [];
const adapter = createAuthAdapter({
  verifyHuman: async token => { calls.push(['verifyHuman', token]); return token === 'ok'; },
  signInPassword: async ({ identity }) => { calls.push(['signIn', identity]); return { user: { id: 'u1' }, accessToken: 'token' }; },
  loadProfile: async userId => { calls.push(['profile', userId]); return { role: 'admin', is_enabled: true }; },
  signOutRemote: async () => { calls.push(['signOut']); }
});

const session = await adapter.signIn({ identity: 'admin@example.com', password: 'secret', turnstileToken: 'ok' });
assert.equal(session.user.id, 'u1');
assert.equal(session.profile.role, 'admin');
assert.deepEqual(calls.slice(0, 3), [['verifyHuman', 'ok'], ['signIn', 'admin@example.com'], ['profile', 'u1']]);

const denied = createAuthAdapter({
  verifyHuman: async () => true,
  signInPassword: async () => ({ user: { id: 'u2' }, accessToken: 'token2' }),
  loadProfile: async () => ({ role: 'staff', is_enabled: true }),
  signOutRemote: async () => {}
});
await assert.rejects(() => denied.signIn({ identity: 'staff@example.com', password: 'secret', turnstileToken: 'ok' }), /AUTH_FORBIDDEN/);

const failedHuman = createAuthAdapter({
  verifyHuman: async () => false,
  signInPassword: async () => { throw new Error('must not call'); },
  loadProfile: async () => null,
  signOutRemote: async () => {}
});
await assert.rejects(() => failedHuman.signIn({ identity: 'a', password: 'b', turnstileToken: 'bad' }), /HUMAN_VERIFICATION_FAILED/);
console.log('auth-adapter: ok');
