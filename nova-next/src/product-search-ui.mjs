function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function clear(node) {
  while (node?.firstChild) node.firstChild.remove();
}

function money(value) {
  const amount = Number(value);
  return Number.isFinite(amount) && amount > 0
    ? new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD', maximumFractionDigits: 0 }).format(amount)
    : 'Price unavailable';
}

export function createProductSearchUi({ featureRuntime, documentObj = globalThis.document, onToast = () => {}, onError = () => {} } = {}) {
  if (!featureRuntime) throw new TypeError('FEATURE_RUNTIME_REQUIRED');
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');
  let bound = false;

  async function searchProducts(query) {
    const q = String(query || '').trim();
    if (!q) throw new TypeError('SEARCH_QUERY_REQUIRED');
    const [catalogue, market] = await Promise.allSettled([
      featureRuntime.productCatalogue(q, { limit: 20 }),
      featureRuntime.marketSearch(q, { limit: 20 })
    ]);
    return Object.freeze({ catalogue, market, query: q });
  }

  function appendMarketCards(root, response) {
    const sources = [
      ['eBay AU', response?.ebay?.items],
      ['Retail references', response?.webRetail?.items],
      ['Gumtree', response?.gumtree?.items],
      ['Facebook Marketplace', response?.facebook?.items]
    ];
    let count = 0;
    for (const [source, items] of sources) {
      for (const item of (Array.isArray(items) ? items : []).slice(0, 6)) {
        count += 1;
        const card = el(documentObj, 'article', 'feature-card');
        card.append(el(documentObj, 'small', '', source), el(documentObj, 'strong', '', item.title || 'Listing'));
        card.append(el(documentObj, 'p', '', `${money(item.deliveredPrice || item.price)} · ${item.condition || item.store || item.seller || 'Source listing'}`));
        if (item.url) {
          const link = el(documentObj, 'a', 'link-button', 'Open source');
          link.href = item.url;
          link.target = '_blank';
          link.rel = 'noopener noreferrer';
          card.append(link);
        }
        root.append(card);
      }
    }
    return count;
  }

  function appendCatalogueCards(root, response) {
    const entries = Array.isArray(response?.items) ? response.items : [];
    for (const entry of entries.slice(0, 10)) {
      const device = entry?.device || {};
      const prices = Array.isArray(entry?.prices) ? entry.prices : [];
      const card = el(documentObj, 'article', 'feature-card');
      card.append(el(documentObj, 'small', '', `${device.category || 'Device'} · ${device.brand || 'Unknown brand'}`));
      card.append(el(documentObj, 'strong', '', device.model_name || device.model_number || 'Catalogue device'));
      const detail = [device.model_number, device.release_year, device.market_region].filter(Boolean).join(' · ');
      if (detail) card.append(el(documentObj, 'p', '', detail));
      const priced = prices
        .map(price => Number(price?.price_aud))
        .filter(price => Number.isFinite(price) && price > 0)
        .sort((a, b) => a - b);
      if (priced.length) card.append(el(documentObj, 'p', '', `Authoritative buy-price reference from ${money(priced[0])}`));
      root.append(card);
    }
    return entries.length;
  }

  function openSearch() {
    documentObj.querySelector('.feature-sheet-backdrop')?.remove();
    const backdrop = el(documentObj, 'div', 'feature-sheet-backdrop');
    const sheet = el(documentObj, 'section', 'feature-sheet');
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    sheet.setAttribute('aria-label', 'Search products & prices');

    const head = el(documentObj, 'div', 'feature-sheet-head');
    head.append(el(documentObj, 'h2', '', 'Search products & prices'));
    const close = el(documentObj, 'button', 'icon-button', '×');
    close.type = 'button';
    close.setAttribute('aria-label', 'Close product search');
    close.addEventListener('click', () => backdrop.remove());
    head.append(close);

    const form = el(documentObj, 'form', 'feature-search');
    const input = documentObj.createElement('input');
    input.placeholder = 'Search a phone, console, laptop or device…';
    input.setAttribute('aria-label', 'Product search query');
    const submit = el(documentObj, 'button', 'small-primary', 'Search');
    submit.type = 'submit';
    form.append(input, submit);

    const boundary = el(documentObj, 'p', 'feature-boundary', 'Read-only search. Nova Next can inspect catalogue and market evidence but cannot change buy prices, approve pricing, purchase products or alter listings.');
    const results = el(documentObj, 'div', 'feature-stack');

    form.addEventListener('submit', async event => {
      event.preventDefault();
      const query = input.value.trim();
      if (!query) return;
      clear(results);
      submit.disabled = true;
      submit.textContent = 'Searching…';
      results.append(el(documentObj, 'p', 'muted', 'Checking catalogue and current Australian market sources…'));
      try {
        const response = await searchProducts(query);
        clear(results);
        let total = 0;
        if (response.catalogue.status === 'fulfilled') total += appendCatalogueCards(results, response.catalogue.value);
        else results.append(el(documentObj, 'p', 'live-status warning', 'Catalogue source is temporarily unavailable.'));
        if (response.market.status === 'fulfilled') total += appendMarketCards(results, response.market.value);
        else results.append(el(documentObj, 'p', 'live-status warning', 'Market sources are temporarily unavailable.'));
        if (!total) results.append(el(documentObj, 'p', 'muted', 'No matching product evidence was returned.'));
      } catch (error) {
        clear(results);
        results.append(el(documentObj, 'p', 'live-status error', 'Product search could not complete. No result was fabricated.'));
        onError(error);
      } finally {
        submit.disabled = false;
        submit.textContent = 'Search';
      }
    });

    sheet.append(head, form, boundary, results);
    backdrop.append(sheet);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) backdrop.remove(); });
    documentObj.body.append(backdrop);
    input.focus();
  }

  function bind() {
    if (bound) return;
    bound = true;
    const tools = documentObj.querySelector('.page[data-route="tools"]');
    const button = [...(tools?.querySelectorAll('.list-cards button') || [])]
      .find(candidate => candidate.querySelector('strong')?.textContent?.trim() === 'Price & Product Search');
    if (!button) {
      onToast('Product Search is unavailable in this layout.', 'error');
      return;
    }
    button.addEventListener('click', event => {
      event.preventDefault();
      event.stopImmediatePropagation();
      openSearch();
    });
  }

  return Object.freeze({ bind, openSearch, searchProducts });
}
