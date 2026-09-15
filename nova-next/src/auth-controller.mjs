function isTransientAuthError(error) {
  const code = String(error?.code || '').toUpperCase();
  const message = String(error?.message || error || '').toUpperCase();
  return code === 'AUTH_TIMEOUT'
    || code === 'NETWORK_ERROR'
    || code === 'FETCH_ERROR'
    || code === 'ECONNRESET'
    || code === 'ETIMEDOUT'
    || message.includes('AUTH_TIMEOUT')
    || message.includes('NETWORK')
    || message.includes('FAILED TO FETCH')
    || message.includes('TIMEOUT')
    || message.includes('TIMED OUT');
}

export function createAuthController({ authClient, sessionStore, onState = () => {} } = {}) {
  if (!authClient || typeof authClient.validateAdminProfile !== 'function' || typeof authClient.refreshSession !== 'function' || typeof authClient.signInPassword !== 'function') {
    throw new TypeError('AUTH_CLIENT_REQUIRED');
  }
  if (!sessionStore || typeof sessionStore.load !== 'function' || typeof sessionStore.save !== 'function' || typeof sessionStore.clear !== 'function') {
    throw new TypeError('SESSION_STORE_REQUIRED');
  }
  let session = null;
  let current = Object.freeze({ status: 'locked', session: null, error: null });

  function publish(status, nextSession = session, error = null) {
    current = Object.freeze({ status, session: nextSession || null, error });
    onState(current);
    return current;
  }

  function degradeValidation() {
    session = null;
    return publish('degraded', null, 'AUTH_VALIDATION_UNAVAILABLE');
  }

  async function restore() {
    publish('checking', null, null);
    const stored = sessionStore.load();
    if (!stored) {
      session = null;
      return publish('locked', null, null);
    }

    try {
      if (await authClient.validateAdminProfile(stored)) {
        session = stored;
        return publish('authenticated', session, null);
      }
    } catch (error) {
      if (isTransientAuthError(error)) return degradeValidation();
      throw error;
    }

    let refreshed;
    try {
      refreshed = await authClient.refreshSession(stored);
    } catch (error) {
      if (isTransientAuthError(error)) return degradeValidation();
      throw error;
    }

    if (refreshed) {
      try {
        if (await authClient.validateAdminProfile(refreshed)) {
          session = sessionStore.save(refreshed);
          return publish('authenticated', session, null);
        }
      } catch (error) {
        if (isTransientAuthError(error)) return degradeValidation();
        throw error;
      }
    }

    sessionStore.clear();
    session = null;
    return publish('locked', null, 'SESSION_INVALID');
  }

  async function signIn({ email, password, captchaToken } = {}) {
    publish('checking', null, null);
    try {
      const signed = await authClient.signInPassword({ email, password, captchaToken });
      if (!await authClient.validateAdminProfile(signed)) {
        sessionStore.clear();
        session = null;
        publish('locked', null, 'AUTH_FORBIDDEN');
        throw new Error('AUTH_FORBIDDEN');
      }
      session = sessionStore.save(signed);
      return publish('authenticated', session, null);
    } catch (error) {
      if (String(error?.message || error) !== 'AUTH_FORBIDDEN') publish('locked', null, String(error?.message || error || 'AUTH_FAILED'));
      throw error;
    }
  }

  function signOut() {
    sessionStore.clear();
    session = null;
    return publish('locked', null, null);
  }

  function getAccessToken() {
    return String(session?.access_token || '');
  }

  function state() {
    return current;
  }

  return Object.freeze({ restore, signIn, signOut, getAccessToken, state });
}
