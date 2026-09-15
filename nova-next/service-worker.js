const CACHE = 'nova-next-shell-v5';
const APP_PREFIX = new URL('./', self.location.href).pathname;
const CORE = [
  APP_PREFIX,
  `${APP_PREFIX}index.html`, `${APP_PREFIX}styles.css`, `${APP_PREFIX}accessibility.css`, `${APP_PREFIX}live.css`, `${APP_PREFIX}completion.css`, `${APP_PREFIX}manifest.webmanifest`, `${APP_PREFIX}app.js`,
  `${APP_PREFIX}assets/icons/nova-orb-192.svg`, `${APP_PREFIX}assets/icons/nova-orb-512.svg`, `${APP_PREFIX}assets/icons/nova-orb-maskable-512.svg`,
  ...['action-policy','auth-controller','auth-policy','automation-runtime','automation-store','automation-ui','capabilities','completion-ui','feature-runtime','feature-ui','file-session','live-runtime','local-preferences','navigation','ota-config','product-search-ui','promotion-config','research-ui','router','runtime-config','safe-client-functions','safe-services','session-store','settings-ui','turnstile','voice-input','voice-ui','workspace-runtime','workspace-store','workspace-ui'].map(name => `${APP_PREFIX}src/${name}.mjs`),
  ...['auth-adapter','chat-adapter','code-proposal-adapter','edge-function-client','nova-api','product-search-adapter','read-adapters','supabase-auth-client','vision-adapter'].map(name => `${APP_PREFIX}src/adapters/${name}.mjs`)
];
const STATIC_PATHS = new Set(CORE.map(asset => new URL(asset, self.location.origin).pathname));

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(CORE)));
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    const keys = await caches.keys();
    const hadPrevious = keys.some(key => key.startsWith('nova-next-') && key !== CACHE);
    await Promise.all(keys.filter(key => key.startsWith('nova-next-') && key !== CACHE).map(key => caches.delete(key)));
    await self.clients.claim();
    if (hadPrevious) {
      const clients = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      for (const client of clients) client.postMessage({ type: 'NOVA_WEB_UPDATE_READY', cache: CACHE });
    }
  })());
});

self.addEventListener('message', event => {
  if (event.data?.type === 'SKIP_WAITING') self.skipWaiting();
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin || !STATIC_PATHS.has(url.pathname)) return;

  event.respondWith((async () => {
    try {
      const response = await fetch(request);
      if (response && response.ok) {
        const cache = await caches.open(CACHE);
        await cache.put(request, response.clone());
      }
      return response;
    } catch (error) {
      const cached = await caches.match(request);
      if (cached) return cached;
      throw error;
    }
  })());
});
