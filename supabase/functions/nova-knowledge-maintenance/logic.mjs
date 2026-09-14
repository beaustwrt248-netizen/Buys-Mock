function clean(value, max = 240) {
  return String(value ?? '').trim().replace(/\s+/g, ' ').slice(0, max);
}

function errorRecord(error) {
  if (error instanceof Error) return { message: error.message };
  if (error && typeof error === 'object') return error;
  return { message: String(error ?? '') };
}

export function classifySourceFailure(error) {
  const record = errorRecord(error);
  const code = clean(record.code, 40).toUpperCase();
  const text = [record.message, record.details, record.hint]
    .map((value) => clean(value, 240).toLowerCase())
    .filter(Boolean)
    .join(' | ');

  if (code === '42501' || text.includes('permission denied')) {
    return { retryable: false, kind: 'permission' };
  }
  if (code === '28P01' || code === '28000' || text.includes('authentication failed') || text.includes('not authenticated')) {
    return { retryable: false, kind: 'auth' };
  }
  if (['42P01', '42703', '42883'].includes(code) || text.includes('relation does not exist') || text.includes('column does not exist')) {
    return { retryable: false, kind: 'schema' };
  }
  if (
    text.includes('gateway timeout')
    || text.includes('bad gateway')
    || text.includes('service unavailable')
    || /(^|\D)(502|503|504)(\D|$)/.test(text)
    || text.includes('upstream timeout')
    || text.includes('connection reset')
  ) {
    return { retryable: true, kind: 'transient_gateway' };
  }

  return { retryable: false, kind: 'hard' };
}

export function summarizeSourceFailures(failures, maxItems = 6) {
  return (Array.isArray(failures) ? failures : [])
    .slice(0, Math.max(0, maxItems))
    .map((failure) => {
      const source = clean(failure?.source, 64).replace(/[^a-z0-9_-]/gi, '') || 'unknown';
      const kind = clean(failure?.kind, 64).replace(/[^a-z0-9_-]/gi, '') || 'hard';
      return `source=${source}:${kind}`;
    })
    .join('; ')
    .slice(0, 400);
}
