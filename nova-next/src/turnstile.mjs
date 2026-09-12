const DEFAULT_TIMEOUT_MS = 12000;

export async function loadTurnstile({ windowObj = globalThis.window, documentObj = globalThis.document, timeoutMs = DEFAULT_TIMEOUT_MS } = {}) {
  if (windowObj?.turnstile) return windowObj.turnstile;
  if (!windowObj || !documentObj?.head) throw new Error('TURNSTILE_ENVIRONMENT_UNAVAILABLE');

  return new Promise((resolve, reject) => {
    let settled = false;
    const finish = error => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      if (error) return reject(error);
      if (windowObj.turnstile) return resolve(windowObj.turnstile);
      reject(new Error('TURNSTILE_NOT_INITIALISED'));
    };
    const timer = setTimeout(() => finish(new Error('TURNSTILE_TIMEOUT')), timeoutMs);
    const existing = documentObj.querySelector?.('script[data-nova-next-turnstile]');
    if (existing) {
      existing.addEventListener('load', () => finish(), { once: true });
      existing.addEventListener('error', () => finish(new Error('TURNSTILE_LOAD_FAILED')), { once: true });
      return;
    }
    const script = documentObj.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
    script.async = true;
    script.defer = true;
    script.dataset.novaNextTurnstile = '1';
    script.onload = () => finish();
    script.onerror = () => finish(new Error('TURNSTILE_LOAD_FAILED'));
    documentObj.head.append(script);
  });
}

export function createTurnstileController({ siteKey, loader = () => loadTurnstile() } = {}) {
  if (!siteKey) throw new Error('TURNSTILE_SITE_KEY_REQUIRED');
  if (typeof loader !== 'function') throw new TypeError('TURNSTILE_LOADER_REQUIRED');
  let api = null;
  let widgetId = null;

  async function mount(container, { onToken = () => {}, onExpired = () => {}, onError = () => {} } = {}) {
    api = await loader();
    if (!api || typeof api.render !== 'function') throw new Error('TURNSTILE_API_INVALID');
    widgetId = api.render(container, {
      sitekey: siteKey,
      theme: 'dark',
      callback: token => onToken(String(token || '')),
      'expired-callback': () => onExpired(),
      'error-callback': () => onError()
    });
    return widgetId;
  }

  function reset() {
    if (api && widgetId !== null && typeof api.reset === 'function') api.reset(widgetId);
  }

  return Object.freeze({ mount, reset });
}
