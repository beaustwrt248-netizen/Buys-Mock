const COPY = Object.freeze({
  loading: 'Loading…',
  empty: 'Nothing to show yet.',
  unavailable: 'This information is temporarily unavailable.',
  degraded: 'Some information is unavailable.',
  offline: 'You appear to be offline.',
  protected: 'This capability is protected.'
});

const SUPPORTED = new Set(Object.keys(COPY));

export function stateCopy(kind) {
  const value = String(kind || '');
  return COPY[value] || COPY.unavailable;
}

export function normaliseAsyncState({ status, message, retryable } = {}) {
  const kind = SUPPORTED.has(String(status || '')) ? String(status) : 'unavailable';
  return Object.freeze({
    kind,
    message: String(message || stateCopy(kind)),
    retryable: kind === 'protected' ? false : retryable === true
  });
}
