const MAX_ERROR_LENGTH = 400;

export function boundedBatch(value, fallback = 10, max = 25) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback;
  return Math.min(Math.max(Math.floor(parsed), 1), max);
}

export function retryDelaySeconds(attempt, baseSeconds = 300, maxSeconds = 21600) {
  const safeAttempt = Math.max(1, Math.floor(Number(attempt) || 1));
  return Math.min(maxSeconds, baseSeconds * (2 ** Math.min(safeAttempt - 1, 8)));
}

export function sanitizeError(error) {
  const raw = error instanceof Error ? error.message : String(error ?? 'Unknown catalogue audit error');
  return raw.replace(/(authorization|cookie|password|secret|token|api[_-]?key)\s*[:=]\s*([^\s,;]+)/gi, '$1=[redacted]')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, MAX_ERROR_LENGTH) || 'Unknown catalogue audit error';
}

export function sourceTier(sourceUrl, sourceName = '') {
  let host = '';
  try { host = new URL(sourceUrl).hostname.toLowerCase().replace(/^www\./, ''); } catch { return 5; }
  const name = String(sourceName).toLowerCase();
  const carrier = /(telstra|optus|vodafone)/.test(`${host} ${name}`);
  const retailer = /(kmart|bigw|big w|woolworths|cashconverters|cash converters)/.test(`${host} ${name}`);
  if (carrier) return 3;
  if (retailer) return 4;
  return 2;
}

export function normalizePageText(value) {
  return String(value ?? '')
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/&nbsp;|&#160;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

function normalizedIdentity(value) {
  return String(value ?? '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').replace(/\s+/g, ' ').trim();
}

export function classifySourceEvidence(device, pageText) {
  const text = normalizePageText(pageText);
  if (!text) return { outcome: 'retry', code: 'empty_source_body', field: null };

  const brand = normalizedIdentity(device?.brand);
  const modelName = normalizedIdentity(device?.model_name);
  const modelNumber = normalizedIdentity(device?.model_number);
  const brandSeen = Boolean(brand && text.includes(brand));
  const modelNameSeen = Boolean(modelName && text.includes(modelName));
  const modelNumberSeen = Boolean(modelNumber && text.includes(modelNumber));

  if (!brandSeen || (!modelNameSeen && !modelNumberSeen)) {
    return { outcome: 'blocked', code: 'source_identity_mismatch', field: 'identity' };
  }

  if (!device?.model_number) return { outcome: 'blocked', code: 'missing_model_number_requires_review', field: 'model_number' };
  if (!device?.release_year) return { outcome: 'blocked', code: 'missing_release_year_requires_review', field: 'release_year' };
  if (!device?.key_specs || Object.keys(device.key_specs).length === 0) {
    return { outcome: 'blocked', code: 'missing_key_specs_requires_review', field: 'key_specs' };
  }

  return { outcome: 'verified', code: 'source_identity_confirmed', field: null };
}

export function terminalStatus(status) {
  return ['verified', 'discrepancy', 'blocked', 'failed'].includes(String(status));
}
