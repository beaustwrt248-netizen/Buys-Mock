import { resolveRoute } from './navigation.mjs';

function normalizeHistory(history) {
  if (!history || typeof history !== 'object') return null;
  if (
    typeof history.read !== 'function' ||
    typeof history.push !== 'function' ||
    typeof history.replace !== 'function' ||
    typeof history.subscribe !== 'function'
  ) return null;
  return history;
}

export function createBrowserHistoryAdapter(windowLike = globalThis.window) {
  if (!windowLike?.location || !windowLike?.history) return null;

  const read = () => resolveRoute(windowLike.location.hash);
  const routeHash = route => `#/${resolveRoute(route)}`;

  return Object.freeze({
    read,
    push(route) {
      const nextHash = routeHash(route);
      if (windowLike.location.hash === nextHash) return;
      windowLike.history.pushState(null, '', nextHash);
    },
    replace(route) {
      const nextHash = routeHash(route);
      if (windowLike.location.hash === nextHash) return;
      windowLike.history.replaceState(null, '', nextHash);
    },
    subscribe(listener) {
      if (typeof listener !== 'function' || typeof windowLike.addEventListener !== 'function') return () => {};
      const handlePopState = () => listener(read());
      windowLike.addEventListener('popstate', handlePopState);
      return () => windowLike.removeEventListener?.('popstate', handlePopState);
    }
  });
}

export function createRouter({ onRoute = () => {}, initialRoute = 'home', history = null } = {}) {
  const historyAdapter = normalizeHistory(history);
  let active = resolveRoute(historyAdapter ? historyAdapter.read() : initialRoute);
  const listeners = new Set();

  if (historyAdapter) historyAdapter.replace(active);

  function emit(route, previous, meta) {
    onRoute(route, previous, meta);
    for (const listener of listeners) listener(route, previous, meta);
  }

  function go(route, { historyMode = 'push', source = 'user' } = {}) {
    const next = resolveRoute(route);
    if (next === active) return active;
    const previous = active;
    active = next;
    if (historyAdapter && historyMode === 'push') historyAdapter.push(active);
    else if (historyAdapter && historyMode === 'replace') historyAdapter.replace(active);
    emit(active, previous, Object.freeze({ source }));
    return active;
  }

  const stopHistory = historyAdapter
    ? historyAdapter.subscribe(route => go(route, { historyMode: 'none', source: 'history' }))
    : () => {};

  function current() {
    return active;
  }

  function subscribe(listener) {
    if (typeof listener !== 'function') return () => {};
    listeners.add(listener);
    return () => listeners.delete(listener);
  }

  function destroy() {
    stopHistory();
    listeners.clear();
  }

  return Object.freeze({ go, current, subscribe, destroy });
}
