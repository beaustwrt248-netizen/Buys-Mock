const KEY = 'nova-next.preferences.v1';
const APPEARANCES = new Set(['system', 'dark', 'light']);
const DEFAULTS = Object.freeze({ appearance: 'system', notifications: false });

function normalise(value) {
  if (!value || typeof value !== 'object') return { ...DEFAULTS };
  return {
    appearance: APPEARANCES.has(value.appearance) ? value.appearance : DEFAULTS.appearance,
    notifications: value.notifications === true
  };
}

export function createPreferencesStore({ storage = globalThis.localStorage } = {}) {
  if (!storage) throw new TypeError('PREFERENCES_STORAGE_REQUIRED');
  let state;
  try {
    const raw = storage.getItem(KEY);
    state = raw ? normalise(JSON.parse(raw)) : { ...DEFAULTS };
  } catch {
    state = { ...DEFAULTS };
  }

  const snapshot = () => Object.freeze({ ...state });

  function persist(next) {
    try {
      storage.setItem(KEY, JSON.stringify(next));
    } catch (error) {
      const wrapped = new Error('PREFERENCES_WRITE_FAILED');
      wrapped.cause = error;
      throw wrapped;
    }
    state = next;
    return snapshot();
  }

  function setAppearance(value) {
    if (!APPEARANCES.has(value)) throw new TypeError('APPEARANCE_INVALID');
    return persist({ ...state, appearance: value });
  }

  function setNotifications(value) {
    return persist({ ...state, notifications: value === true });
  }

  return Object.freeze({ get: snapshot, setAppearance, setNotifications });
}
