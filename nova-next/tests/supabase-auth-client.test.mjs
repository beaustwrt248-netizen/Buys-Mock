import assert from 'node:assert/strict';
import { createSupabaseAuthClient } from '../src/adapters/supabase-auth-client.mjs';

function response(status, body) {
  return { ok: status >= 200 && status < 300, status, async json() { return body; } };
}

const calls = [];
const fetchImpl = async (url, init = {}) => {
  calls.push({ url, init });
  if (url.endsWith('/auth/v1/token?grant_type=password')) return response(200, { access_token: 'a', refresh_token: 'r', user: { id: 'u1', email: 'admin@example.com' } });
  if (url.endsWith('/auth/v1/user')) return response(200, { id: 'u1', email: 'admin@example.com' });
  if (url.includes('/rest/v1/profiles?')) return response(200, [{ role: 'admin', is_enabled: true }]);
  if (url.endsWith('/auth/v1/token?grant_type=refresh_token')) return response(200, { access_token: 'a2', refresh_token: 'r2', user: { id: 'u1' } });
  throw new Error('unexpected url ' + url);
};

const client = createSupabaseAuthClient({ baseUrl: 'https://example.supabase.co', publishableKey: 'pk_test', fetchImpl });
await assert.rejects(() => client.signInPassword({ email: 'a@b.com', password: 'x' }), /CAPTCHA_REQUIRED/);
const signed = await client.signInPassword({ email: 'admin@example.com', password: 'secret', captchaToken: 'human' });
assert.equal(signed.access_token, 'a');
const signInBody = JSON.parse(calls[0].init.body);
assert.equal(signInBody.gotrue_meta_security.captcha_token, 'human');
assert.equal(calls[0].init.headers.apikey, 'pk_test');
assert.equal(calls[0].init.headers.Authorization, undefined);
assert.equal(await client.validateAdminProfile(signed), true);
assert.equal(calls.find(c => c.url.endsWith('/auth/v1/user')).init.headers.Authorization, 'Bearer a');
const refreshed = await client.refreshSession(signed);
assert.equal(refreshed.access_token, 'a2');

const staffClient = createSupabaseAuthClient({
  baseUrl: 'https://example.supabase.co', publishableKey: 'pk_test',
  fetchImpl: async url => url.endsWith('/auth/v1/user') ? response(200, { id: 'u2' }) : response(200, [{ role: 'staff', is_enabled: true }])
});
assert.equal(await staffClient.validateAdminProfile({ access_token: 'x', user: { id: 'u2' } }), false);

const errorClient = createSupabaseAuthClient({ baseUrl: 'https://example.supabase.co', publishableKey: 'pk_test', fetchImpl: async () => response(400, { msg: 'Bad login' }) });
await assert.rejects(() => errorClient.signInPassword({ email: 'a@b.com', password: 'bad', captchaToken: 'human' }), /Bad login/);
console.log('supabase-auth-client: ok');
