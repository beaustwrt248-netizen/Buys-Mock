const KEY = 'nova-next.preferences.v1';
const DEFAULTS = Object.freeze({ appearance: 'system', notifications: false });
const APPEARANCES = new Set(['system', 'light', 'dark']);

function clone(value) {
  return { appearance: value.appearance, notifications: value.notifications };
}

export function createLocalPreferences({ storage = globalThis.localStorage } = {}) {
  if (!storage || typeof storage.getItem !== 'function' || typeof storage.setItem !== 'function') {
    throw new TypeError('STORAGE_REQUIRED');
  }

  let state = clone(DEFAULTS);
  const raw = storage.getItem(KEY);
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      if (!APPEARANCES.has(parsed?.appearance) || typeof parsed?.notifications !== 'boolean') throw new Error('PREFERENCES_INVALID');
      state = { appearance: parsed.appearance, notifications: parsed.notifications };
    } catch {
      storage.removeItem(KEY);
      state = clone(DEFAULTS);
    }
  }

  function persist(next) {
    storage.setItem(KEY, JSON.stringify(next));
    state = next;
    return get();
  }

  function get() {
    return Object.freeze(clone(state));
  }

  function setAppearance(value) {
    const appearance = String(value || '');
    if (!APPEARANCES.has(appearance)) throw new Error('APPEARANCE_INVALID');
    return persist({ ...state, appearance });
  }

  function setNotifications(value) {
    return persist({ ...state, notifications: Boolean(value) });
  }

  return Object.freeze({ get, setAppearance, setNotifications });
}

export { KEY as LOCAL_PREFERENCES_KEY };
