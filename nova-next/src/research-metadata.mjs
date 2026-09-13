function cleanText(value) {
  return String(value ?? '').trim();
}

function cleanNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) && number >= 0 ? number : null;
}

export function normaliseKnowledgeContext(value) {
  const input = value && typeof value === 'object' ? value : {};
  const rawItems = Array.isArray(input.items) ? input.items : [];
  const items = rawItems.slice(0, 12).map(item => Object.freeze({
    id: cleanText(item?.id || item?.chunk_id),
    title: cleanText(item?.title) || 'Evidence',
    category: cleanText(item?.category),
    sourceLabel: cleanText(item?.source_label || item?.source),
    trustLevel: cleanText(item?.trust_level || item?.trust),
    confidence: cleanNumber(item?.confidence ?? item?.score)
  }));
  const declared = cleanNumber(input.count);
  const count = declared === null ? items.length : Math.floor(declared);
  const used = input.used === true && count > 0 && items.length > 0;
  return Object.freeze({
    used,
    count: used ? count : 0,
    semantic: input.semantic === true,
    degraded: input.degraded === true,
    items: used ? Object.freeze(items) : Object.freeze([])
  });
}
