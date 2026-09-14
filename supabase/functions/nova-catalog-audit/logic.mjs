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

function normalizedIdentity(value) {
  return String(value ?? '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').replace(/\s+/g, ' ').trim();
}

function modelIdentityCandidates(modelName, releaseYear) {
  const normalized = normalizedIdentity(modelName);
  if (!normalized) return [];
  const candidates = [normalized];
  const year = String(releaseYear ?? '').trim();
  if (/^\d{4}$/.test(year) && normalized.endsWith(` ${year}`)) {
    const withoutDisplayYear = normalized.slice(0, -(year.length + 1)).trim();
    if (withoutDisplayYear) candidates.push(withoutDisplayYear);
  }
  return candidates;
}

export function isSafePublicSourceUrl(value) {
  try {
    const url = new URL(String(value || ''));
    if (!['https:', 'http:'].includes(url.protocol) || url.username || url.password) return false;
    const host = url.hostname.toLowerCase().replace(/^\[|\]$/g, '').replace(/\.$/, '');
    if (!host || host === 'localhost' || host.endsWith('.localhost') || host.endsWith('.local')) return false;
    if (host.includes(':')) {
      return !(host === '::1' || host.startsWith('fe80:') || host.startsWith('fc') || host.startsWith('fd'));
    }
    const parts = host.split('.');
    if (parts.length === 4 && parts.every((part) => /^\d+$/.test(part))) {
      const octets = parts.map(Number);
      if (octets.some((part) => part < 0 || part > 255)) return false;
      const [a, b] = octets;
      if (a === 0 || a === 10 || a === 127 || a >= 224) return false;
      if (a === 100 && b >= 64 && b <= 127) return false;
      if (a === 169 && b === 254) return false;
      if (a === 172 && b >= 16 && b <= 31) return false;
      if (a === 192 && (b === 0 || b === 168)) return false;
      if (a === 198 && (b === 18 || b === 19)) return false;
    }
    return true;
  } catch {
    return false;
  }
}

export function resolveSafeRedirect(currentUrl, location) {
  if (!location) return null;
  try {
    const resolved = new URL(String(location), String(currentUrl)).toString();
    return isSafePublicSourceUrl(resolved) ? resolved : null;
  } catch {
    return null;
  }
}

export function sourceTier(sourceUrl, sourceName = '', brand = '') {
  let host = '';
  try { host = new URL(sourceUrl).hostname.toLowerCase().replace(/^www\./, ''); } catch { return 5; }
  const name = String(sourceName).toLowerCase();
  const brandKey = normalizedIdentity(brand).replace(/\s+/g, '');
  const hostLabels = host.split('.').map((part) => part.replace(/[^a-z0-9]/g, '')).filter(Boolean);
  const nameKey = normalizedIdentity(sourceName).replace(/\s+/g, '');
  if (brandKey && (hostLabels.includes(brandKey) || nameKey === brandKey || nameKey.startsWith(`${brandKey}support`))) return 1;
  if (/(telstra|optus|vodafone)/.test(`${host} ${name}`)) return 3;
  if (/(kmart|bigw|big w|woolworths|cashconverters|cash converters)/.test(`${host} ${name}`)) return 4;
  return 5;
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

export function classifySourceEvidence(device, pageText) {
  const text = normalizedIdentity(normalizePageText(pageText));
  if (!text) return { outcome: 'retry', code: 'empty_source_body', field: null };

  const brand = normalizedIdentity(device?.brand);
  const modelNames = modelIdentityCandidates(device?.model_name, device?.release_year);
  const modelNumber = normalizedIdentity(device?.model_number);
  const brandSeen = Boolean(brand && text.includes(brand));
  const modelNameSeen = modelNames.some((candidate) => text.includes(candidate));
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
