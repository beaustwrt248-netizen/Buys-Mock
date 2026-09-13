import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const template = readFileSync(new URL('../admin/workspace-template.html', import.meta.url), 'utf8');
const workspaceShell = readFileSync(new URL('../admin/workspace.html', import.meta.url), 'utf8');
const browserAuthBootstrap = readFileSync(new URL('../admin/browser-auth-bootstrap.js', import.meta.url), 'utf8');
const parityCss = readFileSync(new URL('../admin/admin-app-parity.css', import.meta.url), 'utf8');
const parityJs = readFileSync(new URL('../admin/admin-app-parity.js', import.meta.url), 'utf8');
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
  assert.match(browserAuthBootstrap, /\['admin','manager','staff'\]\.includes\(profile\.role\)/);
  assert.match(workspaceShell, /\['admin','manager','staff'\]\.includes\(profile\.role\)/);
  assert.match(workspaceShell, /profile\.role===['"]staff['"]/);
  assert.match(workspaceShell, /admin-support-only-runtime\.js/);
  assert.match(parityJs, /role===['"]staff['"]/);
  assert.match(parityJs, /supportOnly/);
  assert.match(parityJs, /openWorkspace\(['"]support['"]\)/);
  assert.match(supportOnlyRuntime, /support-only/i);
  assert.match(supportOnlyRuntime, /auth\.signOut\(\)/);
  assert.doesNotMatch(supportOnlyRuntime, /profiles.*select\('\*'\)|admin_set_config|notification_jobs|app_config/);
});

test('full-access workspace loads app-parity runtime after core app runtime', () => {
  assert.match(workspaceShell, /client\.auth\.getSession\(\)/);
  assert.match(workspaceShell, /from\('profiles'\)/);
  assert.match(workspaceShell, /admin-app-parity\.js/);
  assert.match(workspaceShell, /app\.js/);
  assert.ok(workspaceShell.indexOf('app.js') < workspaceShell.indexOf('admin-app-parity.js'));
});
