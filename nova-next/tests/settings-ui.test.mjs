import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const settingsUrl = new URL('../src/settings-ui.mjs', import.meta.url);
const app = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const sw = fs.readFileSync(new URL('../service-worker.js', import.meta.url), 'utf8');

await test('settings UI owns Account Appearance and Notifications actions', () => {
  assert.equal(fs.existsSync(settingsUrl), true, 'settings-ui.mjs should exist');
  const ui = fs.readFileSync(settingsUrl, 'utf8');
  assert.match(ui, /createSettingsUi/);
  assert.match(ui, /settings-account/);
  assert.match(ui, /settings-appearance/);
  assert.match(ui, /settings-notifications/);
  assert.match(ui, /setAppearance/);
  assert.match(ui, /setNotifications/);
});

await test('appearance is local and notification setting never claims system push', () => {
  assert.equal(fs.existsSync(settingsUrl), true, 'settings-ui.mjs should exist');
  const ui = fs.readFileSync(settingsUrl, 'utf8');
  assert.match(ui, /novaAppearance/);
  assert.match(ui, /local preference/i);
  assert.doesNotMatch(ui, /Notification\.requestPermission|PushManager|serviceWorker\.ready/);
});

await test('app and offline shell wire isolated preferences/settings modules', () => {
  assert.match(app, /createLocalPreferences/);
  assert.match(app, /createSettingsUi/);
  assert.match(app, /settingsUi\.bind\(\)/);
  assert.match(sw, /src\/local-preferences\.mjs/);
  assert.match(sw, /src\/settings-ui\.mjs/);
});

console.log('settings-ui: ok');
