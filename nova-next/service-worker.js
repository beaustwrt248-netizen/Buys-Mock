const CACHE = 'nova-next-dev-v1';
const APP_PREFIX = '/nova-next/';
const CORE = [
  APP_PREFIX,
  `${APP_PREFIX}index.html`,
  `${APP_PREFIX}styles.css`,
  `${APP_PREFIX}app.js`,
  `${APP_PREFIX}src/navigation.mjs`,
  `${APP_PREFIX}src/router.mjs`
];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(CORE)));
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
  if (url.origin !== self.location.origin || !url.pathname.startsWith(APP_PREFIX)) return;

  event.respondWith(
    fetch(request)
      .then(response => {
        if (response.ok) {
          const copy = response.clone();
          caches.open(CACHE).then(cache => cache.put(request, copy));
        }
        return response;
      })
      .catch(() => caches.match(request).then(hit => hit || caches.match(`${APP_PREFIX}index.html`)))
  );
});
