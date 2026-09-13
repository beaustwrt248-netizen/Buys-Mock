import assert from 'node:assert/strict';
import { PRIMARY_NAV, DRAWER_NAV, resolveRoute } from '../src/navigation.mjs';

assert.deepEqual(PRIMARY_NAV, ['Home', 'Chat', 'Tools', 'Tasks', 'More']);
assert.deepEqual(DRAWER_NAV, [
  'Home', 'Chat', 'Tools', 'Tasks', 'Projects', 'Knowledge Base', 'Files',
  'Automation', 'Calendar', 'Integrations', 'Settings', 'Help & Support'
]);
assert.equal(resolveRoute('home'), 'home');
assert.equal(resolveRoute('Knowledge Base'), 'knowledge');
assert.equal(resolveRoute('Help & Support'), 'help');
assert.equal(resolveRoute('unknown'), 'home');
console.log('layout-contract: ok');
