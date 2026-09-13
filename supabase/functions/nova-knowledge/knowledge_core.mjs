const SENSITIVE_KEY = /(?:^|_)(?:api[_-]?key|authorization|cookie|password|secret|token|access[_-]?token|refresh[_-]?token|session|imei|serial(?:[_-]?number)?|user[_-]?id|account[_-]?id|device[_-]?id|ticket[_-]?id|assigned[_-]?to|approved[_-]?by|created[_-]?by|updated[_-]?by|dispatch[_-]?token)(?:$|_)/i;

const clamp = (value, min = 0, max = 1) => Math.min(max, Math.max(min, Number(value) || 0));
const cleanInline = (value, max = 220) => String(value ?? '').trim().replace(/\s+/g, ' ').slice(0, max);
const cleanContent = (value) => String(value ?? '')
  .replace(/\r\n?/g, '\n')
  .split('\n')
  .map((line) => line.replace(/[ \t]+$/g, ''))
  .join('\n')
  .trim()
  .slice(0, 250000);

export async function sha256Hex(value) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(String(value ?? '')));
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, '0')).join('');
}

export function sanitizeMetadata(value, depth = 0) {
  if (depth > 8 || value == null) return value == null ? value : undefined;
  if (Array.isArray(value)) return value.map((item) => sanitizeMetadata(item, depth + 1)).filter((item) => item !== undefined);
  if (typeof value !== 'object') return value;

  const output = {};
  for (const [key, item] of Object.entries(value)) {
    if (SENSITIVE_KEY.test(key)) continue;
    const sanitized = sanitizeMetadata(item, depth + 1);
    if (sanitized !== undefined) output[key] = sanitized;
  }
  return output;
}

export async function normalizeKnowledgeDocument(input = {}) {
  const category = cleanInline(input.category, 40).toLowerCase() || 'general';
  const title = cleanInline(input.title, 180);
  const content = cleanContent(input.content);
  const source_type = cleanInline(input.source_type, 40).toLowerCase() || 'manual';
  const source_label = cleanInline(input.source_label, 220) || null;
  const source_filename = cleanInline(input.source_filename, 220) || null;
  const version_label = cleanInline(input.version_label, 120) || null;
  const trust_level = cleanInline(input.trust_level, 40).toLowerCase() || 'reference';
  const metadata = sanitizeMetadata(input.metadata && typeof input.metadata === 'object' ? input.metadata : {}) || {};
  const sourceIdentity = [source_type, source_label ?? '', source_filename ?? '', cleanInline(metadata.source_uri, 500), title].join('|');

  return {
    category,
    title,
    content,
    source_type,
    source_label,
    source_filename,
    version_label,
    trust_level,
    metadata,
    content_hash: await sha256Hex(content),
    source_key: `${source_type}:${(await sha256Hex(sourceIdentity)).slice(0, 32)}`,
  };
}

export async function chunkKnowledgeDocument(document, options = {}) {
  const maxChars = Math.max(256, Math.min(Number(options.maxChars) || 1800, 8000));
  const overlapChars = Math.max(0, Math.min(Number(options.overlapChars) || 180, Math.floor(maxChars / 3)));
  const text = cleanContent(document?.content);
  if (!text) return [];

  const chunks = [];
  let start = 0;
  let chunkIndex = 0;
  while (start < text.length) {
    const end = Math.min(text.length, start + maxChars);
    const content = text.slice(start, end);
    chunks.push({
      chunk_index: chunkIndex,
      content,
      content_hash: await sha256Hex(content),
      char_start: start,
      char_end: end,
      token_estimate: Math.max(1, Math.ceil(content.length / 4)),
    });
    if (end >= text.length) break;
    start = end - overlapChars;
    chunkIndex += 1;
  }
  return chunks;
}

function trustScore(level) {
  if (level === 'verified') return 1;
  if (level === 'reviewed') return 0.82;
  return 0.65;
}

function freshnessScore(row, now) {
  const staleAfter = Date.parse(row?.stale_after ?? '');
  if (Number.isFinite(staleAfter) && staleAfter < now) return 0.2;
  const observed = Date.parse(row?.observed_at ?? '');
  if (!Number.isFinite(observed)) return 0.7;
  const ageDays = Math.max(0, (now - observed) / 86400000);
  if (ageDays <= 1) return 1;
  if (ageDays <= 30) return 0.85;
  if (ageDays <= 180) return 0.65;
  return 0.45;
}

export function fuseKnowledgeScores(rows = [], options = {}) {
  const now = Number.isFinite(options.now) ? Number(options.now) : Date.now();
  return rows.map((row) => {
    const lexical = clamp(row.lexical_score);
    const semantic = row.semantic_score == null ? null : clamp(row.semantic_score);
    const authority = trustScore(String(row.trust_level ?? 'reference').toLowerCase());
    const freshness = freshnessScore(row, now);
    const confidence = clamp(row.confidence == null ? 0.7 : row.confidence);
    const hybrid = semantic == null
      ? lexical * 0.72 + authority * 0.13 + freshness * 0.07 + confidence * 0.08
      : lexical * 0.44 + semantic * 0.32 + authority * 0.10 + freshness * 0.07 + confidence * 0.07;
    return { ...row, semantic_score: semantic, hybrid_score: Number(hybrid.toFixed(6)) };
  }).sort((a, b) => b.hybrid_score - a.hybrid_score || String(a.id ?? '').localeCompare(String(b.id ?? '')));
}
