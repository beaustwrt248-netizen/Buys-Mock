const RETAIL_HOSTS = [
  'jbhifi.com.au','officeworks.com.au','apple.com','bigw.com.au','harveynorman.com.au','thegoodguys.com.au',
  'kmart.com.au','target.com.au','costco.com.au','cashconverters.com.au','binglee.com.au','mobileciti.com.au',
  'kogan.com','dicksmith.com.au','umart.com.au','scorptec.com.au','centrecom.com.au','mwave.com.au','pccasegear.com',
  'telstra.com.au','optus.com.au','vodafone.com.au','samsung.com','lenovo.com','dell.com','hp.com','asus.com',
  'acer.com','microsoft.com','sony.com','oppo.com','vivo.com','realme.com','motorola.com','store.google.com','amazon.com.au'
];

const HARD_BLOCK_HOSTS = [
  'reddit.com','old.reddit.com','ozbargain.com.au','staticice.com.au','getprice.com.au','priceme.com.au',
  'whirlpool.net.au','medium.com','youtube.com','news.com.au','facebook.com'
];

const EDITORIAL_PATH = /\/(?:blog|blogs|news|article|articles|review|reviews|guide|guides|support|community|forum|forums|search|search-results|category|categories|compare|comparison|coupon|coupons|deal|deals)(?:\/|$)/i;
const TRACKING_KEYS = /^(?:utm_[a-z]+|gclid|fbclid|msclkid|ref|ref_|source|campaign)$/i;

function hostMatches(host, allowed) {
  return host === allowed || host.endsWith(`.${allowed}`);
}

function safeUrl(rawUrl) {
  try {
    const u = new URL(String(rawUrl || ''));
    if (!/^https?:$/i.test(u.protocol)) return null;
    return u;
  } catch {
    return null;
  }
}

export function canonicalListingUrl(rawUrl) {
  const u = safeUrl(rawUrl);
  if (!u) return '';
  u.hash = '';
  for (const key of [...u.searchParams.keys()]) if (TRACKING_KEYS.test(key)) u.searchParams.delete(key);
  u.hostname = u.hostname.toLowerCase().replace(/^www\./, '');
  u.pathname = u.pathname.replace(/\/{2,}/g, '/').replace(/\/$/, '') || '/';
  return u.toString();
}

export function classifyMarketUrl(rawUrl) {
  const u = safeUrl(rawUrl);
  if (!u) return { allowed: false, kind: 'blocked', reason: 'invalid-url', host: '' };
  const host = u.hostname.toLowerCase().replace(/^www\./, '');
  const path = u.pathname || '/';

  if (hostMatches(host, 'facebook.com')) {
    const marketplaceItem = /^\/marketplace\/item\/[A-Za-z0-9_-]+/i.test(path);
    return marketplaceItem
      ? { allowed: true, kind: 'used-marketplace', reason: 'facebook-marketplace-item', host }
      : { allowed: false, kind: 'blocked', reason: 'facebook-non-listing', host };
  }
  if (hostMatches(host, 'gumtree.com.au')) {
    if (EDITORIAL_PATH.test(path)) return { allowed: false, kind: 'blocked', reason: 'non-product-path', host };
    return { allowed: true, kind: 'used-marketplace', reason: 'gumtree-listing', host };
  }
  if (hostMatches(host, 'ebay.com.au') || hostMatches(host, 'ebay.com')) {
    const listing = /\/itm\//i.test(path);
    return listing
      ? { allowed: true, kind: 'used-marketplace', reason: 'ebay-item', host }
      : { allowed: false, kind: 'blocked', reason: 'ebay-non-listing', host };
  }

  if (HARD_BLOCK_HOSTS.some(x => hostMatches(host, x))) return { allowed: false, kind: 'blocked', reason: 'blocked-source', host };
  if (!RETAIL_HOSTS.some(x => hostMatches(host, x))) return { allowed: false, kind: 'blocked', reason: 'untrusted-source', host };
  if (EDITORIAL_PATH.test(path)) return { allowed: false, kind: 'blocked', reason: 'non-product-path', host };
  return { allowed: true, kind: 'retail-reference', reason: 'trusted-direct-seller', host };
}

export function isAllowedRetailResult(item) {
  if (!item || !Number.isFinite(Number(item.price)) || Number(item.price) <= 0 || !String(item.title || '').trim()) return false;
  const cls = classifyMarketUrl(item.url);
  if (!cls.allowed || cls.kind !== 'retail-reference') return false;
  if (hostMatches(cls.host, 'amazon.com.au')) {
    const seller = String(item.seller || '').trim();
    if (!seller || /^(amazon|amazon au|amazon\.com\.au)$/i.test(seller)) return false;
  }
  return true;
}

export function isAllowedMarketplaceResult(item, source) {
  if (!item || !Number.isFinite(Number(item.price)) || Number(item.price) <= 0 || !String(item.title || '').trim()) return false;
  const cls = classifyMarketUrl(item.url);
  if (!cls.allowed || cls.kind !== 'used-marketplace') return false;
  if (source === 'facebook') return cls.reason === 'facebook-marketplace-item';
  if (source === 'gumtree') return cls.reason === 'gumtree-listing';
  if (source === 'ebay') return cls.reason === 'ebay-item';
  return true;
}

export function dedupeListings(items) {
  const seen = new Set();
  const out = [];
  for (const item of items || []) {
    const url = canonicalListingUrl(item?.url || '');
    const title = String(item?.title || '').toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
    const key = url || `${title}|${Math.round(Number(item?.price) || 0)}`;
    if (!key || seen.has(key)) continue;
    seen.add(key);
    out.push({ ...item, url: url || item.url });
  }
  return out;
}
