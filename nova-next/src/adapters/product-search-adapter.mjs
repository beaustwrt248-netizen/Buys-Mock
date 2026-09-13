const DEFAULT_LIMIT = 20;

function requireEdge(edgeClient) {
  if (!edgeClient || typeof edgeClient.invoke !== 'function') throw new TypeError('EDGE_CLIENT_REQUIRED');
  return edgeClient;
}

function cleanQuery(query) {
  const value = String(query || '').trim();
  if (!value) throw new Error('QUERY_REQUIRED');
  return value.slice(0, 180);
}

function clampLimit(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return DEFAULT_LIMIT;
  return Math.min(40, Math.max(5, Math.round(number)));
}

function normaliseText(value) {
  return String(value || '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').replace(/\s+/g, ' ').trim();
}

function queryTerms(query) {
  return normaliseText(query).split(' ').filter(Boolean);
}

function deviceHaystack(device) {
  const storage = Array.isArray(device?.storage_options) ? device.storage_options.join(' ') : '';
  const ram = Array.isArray(device?.ram_options) ? device.ram_options.join(' ') : '';
  return normaliseText([
    device?.brand,
    device?.family,
    device?.model_name,
    device?.model_number,
    device?.category,
    storage,
    ram,
    device?.market_region
  ].join(' '));
}

function deviceMatches(device, terms) {
  const haystack = deviceHaystack(device);
  return terms.every(term => haystack.includes(term));
}

export function createProductSearchAdapter({ edgeClient } = {}) {
  const edge = requireEdge(edgeClient);

  async function catalogue(query, { limit = DEFAULT_LIMIT } = {}) {
    const clean = cleanQuery(query);
    const terms = queryTerms(clean);
    const response = await edge.invoke('app-pricing-catalogue', {});
    const devices = Array.isArray(response?.devices) ? response.devices : [];
    const prices = Array.isArray(response?.prices) ? response.prices : [];
    const pricesByDevice = new Map();
    for (const price of prices) {
      const id = String(price?.device_catalog_id || '');
      if (!id) continue;
      if (!pricesByDevice.has(id)) pricesByDevice.set(id, []);
      pricesByDevice.get(id).push(Object.freeze({ ...price }));
    }
    const max = clampLimit(limit);
    const items = devices
      .filter(device => deviceMatches(device, terms))
      .slice(0, max)
      .map(device => Object.freeze({
        device: Object.freeze({ ...device }),
        prices: Object.freeze((pricesByDevice.get(String(device?.id || '')) || []).slice())
      }));
    return Object.freeze({
      query: clean,
      items: Object.freeze(items),
      totalDevices: Number(response?.device_count || devices.length || 0)
    });
  }

  async function market(query, { limit = DEFAULT_LIMIT } = {}) {
    const clean = cleanQuery(query);
    return edge.invoke('market-search-v2', { query: clean, limit: clampLimit(limit) });
  }

  return Object.freeze({ catalogue, market });
}
