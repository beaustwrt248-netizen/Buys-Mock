import { resolveRoute } from './navigation.mjs';

export function createRouter({ onRoute = () => {}, initialRoute = 'home' } = {}) {
  let active = resolveRoute(initialRoute);
  const listeners = new Set();

  function emit(route, previous) {
    onRoute(route, previous);
    for (const listener of listeners) listener(route, previous);
  }

  function go(route) {
    const next = resolveRoute(route);
    if (next === active) return active;
    const previous = active;
    active = next;
    emit(active, previous);
    return active;
  }

  function current() {
    return active;
  }

  function subscribe(listener) {
    if (typeof listener !== 'function') return () => {};
    listeners.add(listener);
    return () => listeners.delete(listener);
  }

  return Object.freeze({ go, current, subscribe });
}
