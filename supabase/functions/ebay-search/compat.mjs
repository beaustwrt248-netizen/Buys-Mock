function values(items) {
  return items
    .map(item => Number(item?.deliveredPrice ?? item?.price ?? 0))
    .filter(value => Number.isFinite(value) && value > 0)
    .sort((a, b) => a - b);
}

function percentile(sorted, p) {
  if (!sorted.length) return 0;
  if (sorted.length === 1) return sorted[0];
  const index = (sorted.length - 1) * p;
  const lower = Math.floor(index), upper = Math.ceil(index);
  if (lower === upper) return sorted[lower];
  const weight = index - lower;
  return sorted[lower] * (1 - weight) + sorted[upper] * weight;
}

function usedPricing(items) {
  const sorted = values(items);
  return {
    typicalUsed: percentile(sorted, 0.5),
    p25: percentile(sorted, 0.25),
    p75: percentile(sorted, 0.75),
    lowest: sorted[0] || 0,
    highest: sorted.at(-1) || 0,
  };
}

function retailPricing(items) {
  const sorted = values(items);
  return {
    typicalNew: percentile(sorted, 0.5),
    competitiveLow: percentile(sorted, 0.25),
    competitiveHigh: percentile(sorted, 0.75),
  };
}

export function mergeUsedSources(payload) {
  const ebay = Array.isArray(payload?.ebay?.items) ? payload.ebay.items : [];
  const gumtree = Array.isArray(payload?.gumtree?.items) ? payload.gumtree.items : [];
  const facebook = Array.isArray(payload?.facebook?.items) ? payload.facebook.items : [];
  const seen = new Set();
  const items = [];
  for (const item of [...ebay, ...gumtree, ...facebook]) {
    const title = String(item?.title || '').trim();
    const price = Number(item?.deliveredPrice ?? item?.price ?? 0);
    const url = String(item?.url || item?.itemWebUrl || '').trim();
    if (!title || !Number.isFinite(price) || price <= 0) continue;
    const key = `${url || title.toLowerCase()}|${Math.round(price)}`;
    if (seen.has(key)) continue;
    seen.add(key);
    items.push({
      ...item,
      title,
      price: Number(item?.price ?? price),
      deliveredPrice: price,
      url: url || null,
      source: item?.source || item?.seller || 'Used marketplace',
    });
  }
  return items;
}

export function toLegacyMarketResponse(payload, inputQuery = '') {
  const usedItems = mergeUsedSources(payload);
  const retailItems = Array.isArray(payload?.webRetail?.items)
    ? payload.webRetail.items
    : Array.isArray(payload?.google?.items)
      ? payload.google.items
      : [];

  return {
    success: true,
    inputQuery,
    query: String(payload?.query || inputQuery || ''),
    mode: 'device',
    currency: payload?.currency || 'AUD',
    componentCategory: 'Complete device',
    exactDeviceRequired: false,
    ebay: {
      provider: 'market-search-v2-compat',
      analysedListings: usedItems.length,
      items: usedItems,
      pricing: usedPricing(usedItems),
    },
    google: {
      provider: payload?.retailProvider || payload?.webRetail?.provider || 'market-search-v2',
      analysedListings: retailItems.length,
      items: retailItems,
      pricing: retailPricing(retailItems),
    },
    sourcePolicy: payload?.sourcePolicy || {
      mode: 'trusted-sellers-only',
      rejectsEditorial: true,
      rejectsReddit: true,
      retailIsReferenceOnly: true,
    },
    sourceErrors: payload?.sourceErrors || {},
  };
}
