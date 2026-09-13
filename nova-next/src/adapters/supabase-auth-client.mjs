const DEFAULT_TIMEOUT_MS = 12000;

function requireConfig(name, value) {
  if (!value) throw new Error(`AUTH_CONFIG_${name.toUpperCase()}_MISSING`);
  return String(value).replace(name === 'baseUrl' ? /\/+$/ : /$^/, '');
}

export function createSupabaseAuthClient({ baseUrl, publishableKey, fetchImpl = globalThis.fetch, timeoutMs = DEFAULT_TIMEOUT_MS } = {}) {
  const base = requireConfig('baseUrl', baseUrl).replace(/\/+$/, '');
  const key = requireConfig('publishableKey', publishableKey);
  if (typeof fetchImpl !== 'function') throw new Error('AUTH_CONFIG_FETCH_MISSING');

  function headers(accessToken = '') {
    const value = { apikey: key, Accept: 'application/json' };
    if (accessToken) value.Authorization = `Bearer ${accessToken}`;
    return value;
  }

  async function request(url, init = {}) {
    if (init.signal || typeof AbortController !== 'function') return fetchImpl(url, init);
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      return await fetchImpl(url, { ...init, signal: controller.signal });
    } catch (error) {
      if (controller.signal.aborted) {
        const timeout = new Error('AUTH_TIMEOUT');
        timeout.code = 'AUTH_TIMEOUT';
        throw timeout;
      }
      throw error;
    } finally {
      clearTimeout(timer);
    }
  }

  async function readJson(response) {
    let data = {};
    try { data = await response.json(); } catch {}
    if (!response.ok) {
      const detail = data.msg || data.message || data.error_description || data.error || `AUTH_HTTP_${response.status}`;
      throw new Error(String(detail));
    }
    return data;
  }

  async function signInPassword({ email, password, captchaToken } = {}) {
    if (!captchaToken) throw new Error('CAPTCHA_REQUIRED');
    const response = await request(`${base}/auth/v1/token?grant_type=password`, {
      method: 'POST',
      headers: { ...headers(), 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: String(email || '').trim(),
        password: String(password || ''),
        gotrue_meta_security: { captcha_token: String(captchaToken) }
      }),
      cache: 'no-store'
    });
    return readJson(response);
  }

  async function validateAdminProfile(session) {
    if (!session?.access_token || !session?.user?.id) return false;
    try {
      const userResponse = await request(`${base}/auth/v1/user`, { headers: headers(session.access_token), cache: 'no-store' });
      if (!userResponse.ok) return false;
      const user = await userResponse.json().catch(() => null);
      if (!user?.id || user.id !== session.user.id) return false;

      const profileResponse = await request(
        `${base}/rest/v1/profiles?select=role,is_enabled&id=eq.${encodeURIComponent(session.user.id)}`,
        { headers: headers(session.access_token), cache: 'no-store' }
      );
      if (!profileResponse.ok) return false;
      const rows = await profileResponse.json().catch(() => []);
      const profile = Array.isArray(rows) ? rows[0] : null;
      return profile?.is_enabled === true && String(profile.role || '') === 'admin';
    } catch {
      return false;
    }
  }

  async function refreshSession(session) {
    if (!session?.refresh_token) return null;
    try {
      const response = await request(`${base}/auth/v1/token?grant_type=refresh_token`, {
        method: 'POST',
        headers: { ...headers(), 'Content-Type': 'application/json' },
        body: JSON.stringify({ refresh_token: session.refresh_token }),
        cache: 'no-store'
      });
      if (!response.ok) return null;
      return await response.json();
    } catch {
      return null;
    }
  }

  async function getUser(accessToken) {
    if (!accessToken) return null;
    try {
      const response = await request(`${base}/auth/v1/user`, { headers: headers(accessToken), cache: 'no-store' });
      if (!response.ok) return null;
      return await response.json();
    } catch {
      return null;
    }
  }

  return Object.freeze({ headers, signInPassword, validateAdminProfile, refreshSession, getUser });
}
