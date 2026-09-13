import test from 'node:test';
import assert from 'node:assert/strict';

let moduleUnderTest = null;
try { moduleUnderTest = await import('../src/local-preferences.mjs'); } catch {}

function memoryStorage(seed = {}) {
  const data = new Map(Object.entries(seed));
  return {
    getItem: key => data.has(key) ? data.get(key) : null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: key => data.delete(key),
    snapshot: () => Object.fromEntries(data)
  };
}

await test('local preferences use only the isolated Nova Next namespace', () => {
  assert.ok(moduleUnderTest, 'local-preferences.mjs should exist');
  const storage = memoryStorage();
  const prefs = moduleUnderTest.createLocalPreferences({ storage });
  prefs.setAppearance('dark');
  prefs.setNotifications(true);
  assert.deepEqual(prefs.get(), { appearance: 'dark', notifications: true });
  const snapshot = storage.snapshot();
  assert.equal(typeof snapshot['nova-next.preferences.v1'], 'string');
  assert.equal(Object.keys(snapshot).length, 1);
});

await test('appearance is bounded and notifications are only a local preference', () => {
  assert.ok(moduleUnderTest, 'local-preferences.mjs should exist');
  const prefs = moduleUnderTest.createLocalPreferences({ storage: memoryStorage() });
  assert.throws(() => prefs.setAppearance('neon'), /APPEARANCE_INVALID/);
  assert.deepEqual(prefs.get(), { appearance: 'system', notifications: false });
});

await test('corrupt preferences recover without touching unrelated storage', () => {
  assert.ok(moduleUnderTest, 'local-preferences.mjs should exist');
  const storage = memoryStorage({ 'nova-next.preferences.v1': '{bad', other: 'keep' });
  const prefs = moduleUnderTest.createLocalPreferences({ storage });
  assert.deepEqual(prefs.get(), { appearance: 'system', notifications: false });
  assert.equal(storage.snapshot().other, 'keep');
});

console.log('local-preferences: ok');
