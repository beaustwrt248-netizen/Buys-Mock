const DEFAULT_TIMEOUT_MS = 20000;
const FUNCTION_NAME = /^[a-z0-9][a-z0-9-]{0,79}$/;

export function createEdgeFunctionClient({ baseUrl, publishableKey, getAccessToken, fetchImpl = globalThis.fetch, timeoutMs = DEFAULT_TIMEOUT_MS } = {}) {
  const base = String(baseUrl || '').replace(/\/+$/, '');
  const key = String(publishableKey || '');
  if (!base || !key) throw new Error('EDGE_CONFIG_INCOMPLETE');
  if (typeof getAccessToken !== 'function') throw new TypeError('EDGE_TOKEN_PROVIDER_REQUIRED');
  if (typeof fetchImpl !== 'function') throw new TypeError('EDGE_FETCH_REQUIRED');

  async function invoke(functionName, body = {}) {
    const name = String(functionName || '');
    if (!FUNCTION_NAME.test(name)) throw new Error('INVALID_FUNCTION_NAME');
    const token = await getAccessToken();
    if (!token) throw new Error('AUTH_REQUIRED');

    const controller = typeof AbortController === 'function' ? new AbortController() : null;
    const timer = controller ? setTimeout(() => controller.abort(), timeoutMs) : null;
    try {
      const response = await fetchImpl(`${base}/functions/v1/${name}`, {
        method: 'POST',
        headers: {
          apikey: key,
          Authorization: `Bearer ${token}`,
          Accept: 'application/json',
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body ?? {}),
        cache: 'no-store',
        ...(controller ? { signal: controller.signal } : {})
      });
      let data = {};
      try { data = await response.json(); } catch {}
      if (!response.ok) {
        const message = data?.error || data?.message || `EDGE_HTTP_${response.status}`;
        const error = new Error(String(message));
        error.status = response.status;
        error.payload = data;
        throw error;
      }
      return data;
    } catch (error) {
      if (controller?.signal.aborted) {
        const timeout = new Error('EDGE_TIMEOUT');
        timeout.code = 'EDGE_TIMEOUT';
        throw timeout;
      }
      throw error;
    } finally {
      if (timer) clearTimeout(timer);
    }
  }

  return Object.freeze({ invoke });
}
