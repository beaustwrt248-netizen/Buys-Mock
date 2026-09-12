import assert from 'node:assert/strict';
import { createNovaApi } from '../src/adapters/nova-api.mjs';

const seen = [];
const api = createNovaApi({
  getAccessToken: () => 'token',
  transport: async request => { seen.push(request); return { ok: true, data: { count: 3 }, evidence: [{ id: 'e1' }] }; }
});

const safe = await api.run('catalogue-audit-read', { category: 'mobile' });
assert.equal(safe.ok, true);
assert.equal(seen[0].headers.Authorization, 'Bearer token');
assert.equal(seen[0].action, 'catalogue-audit-read');

const blocked = await api.run('deploy', { target: 'production' });
assert.equal(blocked.ok, false);
assert.equal(blocked.error.code, 'APPROVAL_REQUIRED');
assert.equal(seen.length, 1);

const unknown = await api.run('something-new', {});
assert.equal(unknown.ok, false);
assert.equal(unknown.error.code, 'ACTION_BLOCKED');
console.log('nova-api: ok');
