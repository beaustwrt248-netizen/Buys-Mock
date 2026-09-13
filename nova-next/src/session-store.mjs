const SAFE_KEYS = ['access_token', 'refresh_token', 'expires_in', 'expires_at', 'token_type', 'user'];

export function createSessionStore({ storage, key = 'nova-next.session.v1' } = {}) {
  if (!storage || typeof storage.getItem !== 'function' || typeof storage.setItem !== 'function' || typeof storage.removeItem !== 'function') {
    throw new TypeError('SESSION_STORAGE_REQUIRED');
  }

  function save(session) {
    if (!session || typeof session !== 'object') {
      clear();
      return null;
    }
    const safe = {};
    for (const name of SAFE_KEYS) {
      if (session[name] !== undefined) safe[name] = session[name];
    }
    storage.setItem(key, JSON.stringify(safe));
    return safe;
  }

  function load() {
    const raw = storage.getItem(key);
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== 'object' || !parsed.access_token || !parsed.user?.id) {
        clear();
        return null;
      }
      return parsed;
    } catch {
      clear();
      return null;
    }
  }

  function clear() {
    storage.removeItem(key);
  }

  return Object.freeze({ save, load, clear });
}
