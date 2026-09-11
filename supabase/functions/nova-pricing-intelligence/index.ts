import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const ANON_KEY = Deno.env.get('SUPABASE_ANON_KEY') || '';
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ORIGINS = new Set(['https://buyshub.me','https://www.buyshub.me','https://beaustwrt248-netizen.github.io']);
const clean = (v: unknown, n = 500) => String(v ?? '').trim().slice(0, n);

function headers(req: Request) {
  const origin = req.headers.get('Origin') || '';
  return {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': ORIGINS.has(origin) ? origin : 'https://buyshub.me',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Cache-Control': 'no-store',
    'Vary': 'Origin',
    'X-Content-Type-Options': 'nosniff'
  };
}

async function authorise(req: Request) {
  const token = (req.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
  if (!token) return { error: 'Authentication required', status: 401 } as const;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return { error: 'Invalid session', status: 401 } as const;
  const { data: profile } = await admin.from('profiles').select('role,is_enabled').eq('id', user.id).maybeSingle();
  if (!profile?.is_enabled || profile.role !== 'admin') return { error: 'Admin access required', status: 403 } as const;
  return { user, token } as const;
}

function prices(items: any[]) {
  return (Array.isArray(items) ? items : []).map(x => Number(x?.price)).filter(x => Number.isFinite(x) && x > 0).sort((a,b) => a-b);
}
function median(values: number[]) {
  if (!values.length) return 0;
  const mid = Math.floor(values.length / 2);
  return values.length % 2 ? values[mid] : (values[mid - 1] + values[mid]) / 2;
}

Deno.serve(async req => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === 'OPTIONS') return new Response('ok', { headers: h });
  if (req.method !== 'POST') return reply({ error: 'POST required' }, 405);
  try {
    if (!SUPABASE_URL || !SERVICE_ROLE || !ANON_KEY) return reply({ error: 'Pricing intelligence unavailable' }, 503);
    const caller = await authorise(req);
    if ('error' in caller) return reply({ error: caller.error }, caller.status);
    let body: any = {};
    try { body = await req.json(); } catch { return reply({ error: 'Invalid JSON request' }, 400); }
    const query = clean(body.query, 240);
    if (!query) return reply({ error: 'query is required' }, 400);

    const marketResponse = await fetch(`${SUPABASE_URL}/functions/v1/market-search-v2`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${caller.token}`,
        'apikey': ANON_KEY,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ query, limit: 30 })
    });
    const market = await marketResponse.json().catch(() => ({}));
    if (!marketResponse.ok || !market?.success) return reply({ error: 'Australian market evidence is unavailable right now' }, 503);

    const a = market.pricingAnalysis || {};
    const retailItems = market.webRetail?.items || market.google?.items || [];
    const retailPrices = prices(retailItems);
    const retailMedian = median(retailPrices);
    const retained = Number(a.retainedListings || 0);
    const sourceCount = Number(a.sourceCount || 0);
    const confidence = Number(a.confidence || 0);
    const evidenceAdequate = retained >= 4 && sourceCount >= 1 && Number(a.median || 0) > 0;

    return reply({
      ok: true,
      query,
      currency: 'AUD',
      advisory_only: true,
      protected_pricing_write: true,
      used_market: {
        median: Number(a.median || 0),
        q1: Number(a.q1 || 0),
        q3: Number(a.q3 || 0),
        lower_bound: Number(a.lowerBound || 0),
        upper_bound: Number(a.upperBound || 0),
        raw_listings: Number(a.rawListings || 0),
        retained_listings: retained,
        outliers_removed: Number(a.outliersRemoved || 0),
        source_count: sourceCount,
        confidence,
        confidence_label: clean(a.confidenceLabel || 'none', 20)
      },
      retail_reference: {
        listing_count: retailPrices.length,
        median: Number(retailMedian.toFixed(2)),
        excluded_from_used_market_median: true
      },
      quality: {
        evidence_adequate: evidenceAdequate,
        method: clean(a.method || 'IQR outlier filter across Australian used-market listings', 160),
        note: evidenceAdequate
          ? 'Use the evidence range with device condition, warranty, completeness and Morley margin policy before a human pricing decision.'
          : 'Evidence is too thin for a confident pricing recommendation; gather more comparable Australian listings before changing a price.'
      },
      authority: 'Nova can analyse and recommend, but cannot approve or write protected Morley buy/sell prices from this endpoint.'
    });
  } catch (error) {
    console.error('[nova-pricing-intelligence]', clean(error instanceof Error ? error.message : error, 300));
    return reply({ error: 'Pricing intelligence could not complete this request' }, 500);
  }
});
