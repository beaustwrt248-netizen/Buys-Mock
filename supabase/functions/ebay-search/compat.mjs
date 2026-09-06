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
      pricing: { typicalUsed: 0, p25: 0, p75: 0, lowest: 0, highest: 0 },
    },
    google: {
      provider: payload?.retailProvider || payload?.webRetail?.provider || 'market-search-v2',
      analysedListings: retailItems.length,
      items: retailItems,
      pricing: { typicalNew: 0, competitiveLow: 0, competitiveHigh: 0 },
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
