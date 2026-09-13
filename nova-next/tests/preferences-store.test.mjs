import test from 'node:test';
import assert from 'node:assert/strict';
import { createPreferencesStore } from '../src/preferences-store.mjs';

function memoryStorage(seed = {}) {
  const data = new Map(Object.entries(seed));
  return {
    getItem: key => data.has(key) ? data.get(key) : null,
    setItem: (key, value) => data.set(key, String(value)),
    removeItem: key => data.delete(key),
    dump: () => Object.fromEntries(data)
  };
}

test('preferences default to system appearance and local notifications off', () => {
  const store = createPreferencesStore({ storage: memoryStorage() });
  assert.deepEqual(store.get(), { appearance: 'system', notifications: false });
});

test('preferences persist only approved non-sensitive fields', () => {
  const storage = memoryStorage();
  const store = createPreferencesStore({ storage });
  store.setAppearance('dark');
  store.setNotifications(true);
  assert.deepEqual(store.get(), { appearance: 'dark', notifications: true });
  const raw = storage.dump()['nova-next.preferences.v1'];
  assert.deepEqual(JSON.parse(raw), { appearance: 'dark', notifications: true });
  assert.equal(/token|password|email|secret/i.test(raw), false);
});

test('appearance rejects unsupported values', () => {
  const store = createPreferencesStore({ storage: memoryStorage() });
  assert.throws(() => store.setAppearance('neon'), /APPEARANCE_INVALID/);
  assert.deepEqual(store.get(), { appearance: 'system', notifications: false });
});

test('corrupt storage resets safely', () => {
  const storage = memoryStorage({ 'nova-next.preferences.v1': '{not-json' });
  const store = createPreferencesStore({ storage });
  assert.deepEqual(store.get(), { appearance: 'system', notifications: false });
});

test('write failure does not mutate in-memory state', () => {
  const storage = { getItem: () => null, setItem: () => { throw new Error('quota'); }, removeItem: () => {} };
  const store = createPreferencesStore({ storage });
  assert.throws(() => store.setAppearance('light'), /PREFERENCES_WRITE_FAILED/);
  assert.deepEqual(store.get(), { appearance: 'system', notifications: false });
});
