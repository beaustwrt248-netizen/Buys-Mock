export const WORKSPACE_ROUTE_TIMEOUT_MS = 6000;

export function withWorkspaceRouteDeadline(promise, timeoutMs = WORKSPACE_ROUTE_TIMEOUT_MS) {
  let timer = null;
  const timeout = new Promise((_, reject) => {
    timer = setTimeout(() => {
      const error = new Error('WORKSPACE_ROUTE_TIMEOUT');
      error.code = 'WORKSPACE_ROUTE_TIMEOUT';
      reject(error);
    }, timeoutMs);
  });

  return Promise.race([Promise.resolve(promise), timeout])
    .finally(() => {
      if (timer !== null) clearTimeout(timer);
    });
}

export function createWorkspaceRouteGeneration() {
  let generation = 0;
  return Object.freeze({
    next() {
      generation += 1;
      return generation;
    },
    current() {
      return generation;
    },
    isCurrent(value) {
      return value === generation;
    }
  });
}
