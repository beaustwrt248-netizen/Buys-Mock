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
const targetedNotifications = readFileSync(new URL('../admin/targeted-notifications.js', import.meta.url), 'utf8');
const nativeDashboard = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt', import.meta.url), 'utf8');
const nativeCatalogue = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/CatalogueNativePanel.kt', import.meta.url), 'utf8');
const nativeUsers = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/UserManagementPanel.kt', import.meta.url), 'utf8');

const requiredWorkspaces = [
  ['overview', 'Overview'], ['support', 'Support'], ['catalogue', 'Catalogue'],
  ['health', 'Production health'], ['guardian', 'Guardian'], ['notifications', 'Notifications'],
  ['users-devices', 'Users & devices'], ['staff-alerts', 'Staff alerts'], ['controls', 'Safe controls'],
  ['audit', 'Audit'], ['release', 'Release']
];

const retainedFeatureIds = [
  'metricUsers', 'metricDevices', 'metricVersion', 'metricQueued', 'usersList', 'devicesList',
  'ticketsList', 'ticketDetail', 'catalogueList', 'catalogueStatus', 'catalogueRefreshBtn',
  'featureFlags', 'maintenanceMessage', 'otaEnabled', 'saveControlsBtn',
  'releaseSummary', 'releaseAdoption', 'notifTitle', 'queueNotifBtn', 'annList',
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

test('parity template preserves native-contract DOM hooks only', () => {
  for (const id of retainedFeatureIds) assert.match(template, new RegExp(`id=["']${id}["']`), `missing native-parity feature id ${id}`);
  for (const retiredId of ['pricingEditor','pricingSaveBtn','annTitle','annBody','annAudience','publishAnnBtn','saveReleaseBtn','minName','minCode','forceUpdate']) {
    assert.doesNotMatch(template, new RegExp(`id=["']${retiredId}["']`), `non-native authority must not be rendered: ${retiredId}`);
  }
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

test('full-access loader owns native parity instead of legacy write runtimes', () => {
  assert.match(workspaceShell, /admin-native-parity-core\.js/);
  assert.match(workspaceShell, /catalogue-readonly-parity\.js/);
  assert.doesNotMatch(workspaceShell, /\['adminCoreApp','app\.js/);
  assert.doesNotMatch(workspaceShell, /pricing-management\.js/);
  assert.doesNotMatch(workspaceShell, /release-control\.js/);
  assert.doesNotMatch(workspaceShell, /control-governance\.js/);
  assert.match(workspaceShell, /admin-user-access-parity\.js/);
  assert.doesNotMatch(workspaceShell, /admin-home\.js\?v=8[^\n]*\]/);
  assert.doesNotMatch(workspaceShell, /admin-v2\.js\?v=8[^\n]*\]/);
});

test('Catalogue and read-only native workspaces do not expose legacy mutations', () => {
  assert.match(nativeCatalogue, /This workspace is read-only/);
  assert.match(template, /id="catalogueList"/);
  assert.match(template, /id="catalogueRefreshBtn"/);
  assert.doesNotMatch(template, /pricingSaveBtn|pricingAuthoritative|pricingActive|pricingSource/);
  assert.doesNotMatch(template, /publishAnnBtn|annTitle|annBody|annAudience/);
  assert.doesNotMatch(template, /saveReleaseBtn|forceUpdate|minCode|minName/);
});

test('user access mirrors native controls without destructive or name-edit extras', () => {
  assert.match(nativeUsers, /delete and force-signout are not exposed/);
  assert.doesNotMatch(template, /data-display-name|data-name-save/);
  assert.doesNotMatch(userAccessParity, /data-user-action="force_signout"|data-user-action="delete"/);
  assert.match(userAccessParity, /reset_password/);
  assert.match(userAccessParity, /create_user/);
  assert.match(userAccessParity, /reissue_invite/);
  assert.match(userAccessParity, /admin_revoke_team_invite/);
});

test('notification targets match native audiences and users, not device installations', () => {
  assert.match(targetedNotifications, /audience:all/);
  assert.match(targetedNotifications, /user:/);
  assert.doesNotMatch(targetedNotifications, /device:/);
  assert.doesNotMatch(targetedNotifications, /target_installation_id/);
});

test('full-access workspace loads app-parity navigation after native parity core', () => {
  assert.match(workspaceShell, /client\.auth\.getSession\(\)/);
  assert.match(workspaceShell, /from\('profiles'\)/);
  assert.match(workspaceShell, /admin-app-parity\.js/);
  assert.match(workspaceShell, /admin-native-parity-core\.js/);
  assert.ok(workspaceShell.indexOf('admin-native-parity-core.js') < workspaceShell.indexOf('admin-app-parity.js'));
});
