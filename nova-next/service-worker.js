const CACHE = 'nova-next-shell-v2';
const APP_PREFIX = new URL('./', self.location.href).pathname;
const CORE = [
  APP_PREFIX,
  `${APP_PREFIX}index.html`,
  `${APP_PREFIX}styles.css`,
  `${APP_PREFIX}app.js`,
  `${APP_PREFIX}src/action-policy.mjs`,
  `${APP_PREFIX}src/auth-controller.mjs`,
  `${APP_PREFIX}src/auth-policy.mjs`,
  `${APP_PREFIX}src/capabilities.mjs`,
  `${APP_PREFIX}src/feature-runtime.mjs`,
  `${APP_PREFIX}src/feature-ui.mjs`,
  `${APP_PREFIX}src/file-session.mjs`,
  `${APP_PREFIX}src/live-runtime.mjs`,
  `${APP_PREFIX}src/navigation.mjs`,
  `${APP_PREFIX}src/promotion-config.mjs`,
  `${APP_PREFIX}src/router.mjs`,
  `${APP_PREFIX}src/runtime-config.mjs`,
  `${APP_PREFIX}src/safe-client-functions.mjs`,
  `${APP_PREFIX}src/safe-services.mjs`,
  `${APP_PREFIX}src/session-store.mjs`,
  `${APP_PREFIX}src/turnstile.mjs`,
  `${APP_PREFIX}src/workspace-runtime.mjs`,
  `${APP_PREFIX}src/workspace-store.mjs`,
  `${APP_PREFIX}src/workspace-ui.mjs`,
  `${APP_PREFIX}src/adapters/auth-adapter.mjs`,
  `${APP_PREFIX}src/adapters/chat-adapter.mjs`,
  `${APP_PREFIX}src/adapters/code-proposal-adapter.mjs`,
  `${APP_PREFIX}src/adapters/edge-function-client.mjs`,
  `${APP_PREFIX}src/adapters/nova-api.mjs`,
  `${APP_PREFIX}src/adapters/read-adapters.mjs`,
  `${APP_PREFIX}src/adapters/supabase-auth-client.mjs`,
  `${APP_PREFIX}src/adapters/vision-adapter.mjs`
];
const STATIC_PATHS = new Set(CORE.map(asset => new URL(asset, self.location.origin).pathname));

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(CORE)));
  self.skipWaiting();
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key.startsWith('nova-next-') && key !== CACHE).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin || !STATIC_PATHS.has(url.pathname)) return;

  event.respondWith(
    caches.match(request).then(hit => hit || fetch(request))
  );
});
