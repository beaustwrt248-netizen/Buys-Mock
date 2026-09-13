import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const template = readFileSync(new URL('../admin/workspace-template.html', import.meta.url), 'utf8');
const workspaceShell = readFileSync(new URL('../admin/workspace.html', import.meta.url), 'utf8');
const browserAuthBootstrap = readFileSync(new URL('../admin/browser-auth-bootstrap.js', import.meta.url), 'utf8');
const parityCss = readFileSync(new URL('../admin/admin-app-parity.css', import.meta.url), 'utf8');
const parityJs = readFileSync(new URL('../admin/admin-app-parity.js', import.meta.url), 'utf8');
const userAccessParity = readFileSync(new URL('../admin/admin-user-access-parity.js', import.meta.url), 'utf8');
const supportOnlyRuntime = readFileSync(new URL('../admin/admin-support-only-runtime.js', import.meta.url), 'utf8');
const nativeDashboard = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt', import.meta.url), 'utf8');

const requiredWorkspaces = [
  ['overview', 'Overview'], ['support', 'Support'], ['catalogue', 'Catalogue'],
  ['health', 'Production health'], ['guardian', 'Guardian'], ['notifications', 'Notifications'],
  ['users-devices', 'Users & devices'], ['staff-alerts', 'Staff alerts'], ['controls', 'Safe controls'],
  ['audit', 'Audit'], ['release', 'Release']
];

const retainedFeatureIds = [
  'metricUsers', 'metricDevices', 'metricVersion', 'metricQueued', 'usersList', 'devicesList',
  'ticketsList', 'ticketDetail', 'pricingList', 'pricingEditor', 'featureFlags', 'saveControlsBtn',
  'releaseName', 'saveReleaseBtn', 'notifTitle', 'queueNotifBtn', 'annTitle', 'publishAnnBtn',
  'auditList', 'logoutBtn'
];

test('rebuilt Admin parity runtimes are syntactically valid JavaScript', () => {
  assert.doesNotThrow(() => new Function(parityJs));
  assert.doesNotThrow(() => new Function(userAccessParity));
  assert.doesNotThrow(() => new Function(supportOnlyRuntime));
});

test('web workspace mirrors every primary Android Admin workspace', () => {
  assert.match(template, /data-admin-shell="app-parity"/);
  for (const [key, label] of requiredWorkspaces) {
    assert.match(template, new RegExp(`data-workspace=["']${key}["']`), `missing workspace nav ${label}`);
    assert.match(template, new RegExp(`data-workspace-panel=["']${key}["']`), `missing workspace panel ${label}`);
  }
  for (const label of ['Overview', 'Support', 'Catalogue', 'Health', 'Guardian', 'Notifications', 'Users & devices', 'Staff alerts', 'Controls', 'Audit', 'Release']) {
    assert.match(nativeDashboard, new RegExp(label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }
});

test('rebuild preserves feature-module DOM contracts', () => {
  for (const id of retainedFeatureIds) assert.match(template, new RegExp(`id=["']${id}["']`), `missing retained feature id ${id}`);
});

test('desktop and mobile web share one responsive app shell', () => {
  assert.match(template, /class="admin-app-shell"/);
  assert.match(template, /class="admin-workspace-nav"/);
  assert.match(parityCss, /@media\s*\(max-width:\s*820px\)/);
  assert.match(parityCss, /min-height:\s*44px/);
  assert.match(parityCss, /overflow-x:\s*auto/);
  assert.match(parityJs, /openWorkspace/);
  assert.match(parityJs, /data-workspace-panel/);
});

test('web authorization matches Android entry policy and keeps staff support-only', () => {
  assert.match(browserAuthBootstrap, /FULL_ACCESS_ROLES=\['admin','manager'\]/);
  assert.match(browserAuthBootstrap, /ENTRY_ROLES=\[\.\.\.FULL_ACCESS_ROLES,'staff'\]/);
  assert.match(browserAuthBootstrap, /ENTRY_ROLES\.includes\(profile\.role\)/);
  assert.match(workspaceShell, /FULL_ACCESS_ROLES=\['admin','manager'\]/);
  assert.match(workspaceShell, /ENTRY_ROLES=\[\.\.\.FULL_ACCESS_ROLES,'staff'\]/);
  assert.match(workspaceShell, /ENTRY_ROLES\.includes\(profile\.role\)/);
  assert.match(workspaceShell, /profile\.role===['"]staff['"]/);
  assert.match(workspaceShell, /admin-support-only-runtime\.js/);
  assert.match(workspaceShell, /openWorkspace\?\.\(supportOnly\?['"]support['"]:['"]overview['"]\)/);
  assert.match(parityJs, /role===['"]staff['"]/);
  assert.match(parityJs, /supportOnly/);
  assert.match(parityJs, /if\(supportOnly&&name!==['"]support['"]\)name=['"]support['"]/);
  assert.match(parityJs, /let initial=supportOnly\?['"]support['"]:['"]overview['"]/);
  assert.match(parityJs, /getElementById\(['"]tab-tickets['"]\)\?\.classList\.remove\(['"]hidden['"]\)/);
  assert.match(supportOnlyRuntime, /support-only/i);
  assert.match(supportOnlyRuntime, /auth\.signOut\(\)/);
  assert.doesNotMatch(supportOnlyRuntime, /profiles.*select\('\*'\)|admin_set_config|notification_jobs|app_config/);
});

test('full-access runtime keeps native user-access behavior and removes legacy shell rewrites', () => {
  assert.match(workspaceShell, /admin-user-access-parity\.js/);
  assert.doesNotMatch(workspaceShell, /admin-home\.js\?v=8[^\n]*\]/);
  assert.doesNotMatch(workspaceShell, /admin-v2\.js\?v=8[^\n]*\]/);
  assert.match(workspaceShell, /invites\.js\?v=4/);
  assert.match(workspaceShell, /legacy invite renderer is intentionally not loaded/);
  assert.doesNotMatch(workspaceShell, /\['adminInvites','invites\.js\?v=4'\]/);
  assert.match(template, /id="adminResetUser"/);
  assert.match(template, /id="teamInviteTemporary"/);
  assert.match(userAccessParity, /reset_password/);
  assert.match(userAccessParity, /create_user/);
  assert.match(userAccessParity, /reissue_invite/);
  assert.match(userAccessParity, /admin_revoke_team_invite/);
  assert.match(userAccessParity, /data-user-action="force_signout"/);
  assert.match(userAccessParity, /data-user-action="delete"/);
  assert.match(userAccessParity, /remove\(\)/);
});

test('full-access workspace loads app-parity runtime after core app runtime', () => {
  assert.match(workspaceShell, /client\.auth\.getSession\(\)/);
  assert.match(workspaceShell, /from\('profiles'\)/);
  assert.match(workspaceShell, /admin-app-parity\.js/);
  assert.match(workspaceShell, /app\.js/);
  assert.ok(workspaceShell.indexOf('app.js') < workspaceShell.indexOf('admin-app-parity.js'));
});
