import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { dedupeListings, isAllowedMarketplaceResult, isAllowedRetailResult } from "./source-policy.mjs";

const ALLOWED_ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"]);
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
let ebayToken = "", ebayTokenExp = 0;

function cors(req: Request) {
  const origin = req.headers.get("Origin") || "";
  return {
    "Access-Control-Allow-Origin": ALLOWED_ORIGINS.has(origin) ? origin : "https://buyshub.me",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Content-Type": "application/json",
    "Cache-Control": "no-store",
    "Vary": "Origin",
    "X-Content-Type-Options": "nosniff",
  };
}
function priceFromText(s: string) {
  const m = String(s || "").match(/(?:A\$|AU\$|AUD\s*\$?|\$)\s*([0-9]{2,5}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)/i);
  return m ? Number(m[1].replace(/,/g, "")) : 0;
}
function percentile(values: number[], p: number) {
  if (!values.length) return 0;
  const index = (values.length - 1) * p;
  const lower = Math.floor(index), upper = Math.ceil(index);
  if (lower === upper) return values[lower];
  return values[lower] + (values[upper] - values[lower]) * (index - lower);
}
function itemPrice(item: any, delivered = false) {
  const value = delivered ? Number(item?.deliveredPrice || item?.price) : Number(item?.price || item?.deliveredPrice);
  return Number.isFinite(value) && value > 0 ? value : 0;
}
function usedEvidence(ebayResult: any, gumtree: any[], facebook: any[]) {
  const evidence: { source: string; price: number }[] = [];
  for (const item of ebayResult?.items || []) { const price = itemPrice(item, true); if (price) evidence.push({ source: "ebay", price }); }
  for (const item of gumtree || []) { const price = itemPrice(item); if (price) evidence.push({ source: "gumtree", price }); }
  for (const item of facebook || []) { const price = itemPrice(item); if (price) evidence.push({ source: "facebook", price }); }
  return evidence;
}
function analyseUsedMarket(ebayResult: any, gumtree: any[], facebook: any[]) {
  const evidence = usedEvidence(ebayResult, gumtree, facebook);
  const prices = evidence.map(x => x.price).sort((a, b) => a - b);
  if (!prices.length) return { rawListings: 0, retainedListings: 0, outliersRemoved: 0, median: 0, q1: 0, q3: 0, lowerBound: 0, upperBound: 0, sourceCount: 0, confidence: 0, confidenceLabel: "none" };
  const q1 = percentile(prices, .25), q3 = percentile(prices, .75), iqr = q3 - q1;
  const lowerBound = prices.length >= 5 ? Math.max(1, q1 - 1.5 * iqr) : prices[0];
  const upperBound = prices.length >= 5 ? q3 + 1.5 * iqr : prices[prices.length - 1];
  const retained = evidence.filter(x => x.price >= lowerBound && x.price <= upperBound);
  const retainedPrices = retained.map(x => x.price).sort((a, b) => a - b);
  const sourceCount = new Set(retained.map(x => x.source)).size;
  const median = percentile(retainedPrices, .5);
  const retainedQ1 = percentile(retainedPrices, .25), retainedQ3 = percentile(retainedPrices, .75);
  const spreadRatio = median > 0 ? Math.min(1, Math.max(0, (retainedQ3 - retainedQ1) / median)) : 1;
  const countScore = Math.min(1, retained.length / 12);
  const sourceScore = Math.min(1, sourceCount / 3);
  const spreadScore = 1 - spreadRatio;
  const confidence = Number((countScore * .5 + sourceScore * .3 + spreadScore * .2).toFixed(3));
  const confidenceLabel = confidence >= .75 ? "high" : confidence >= .5 ? "medium" : "low";
  return { rawListings: evidence.length, retainedListings: retained.length, outliersRemoved: evidence.length - retained.length, median: Number(median.toFixed(2)), q1: Number(retainedQ1.toFixed(2)), q3: Number(retainedQ3.toFixed(2)), lowerBound: Number(lowerBound.toFixed(2)), upperBound: Number(upperBound.toFixed(2)), sourceCount, confidence, confidenceLabel };
}
function applyUsedBounds(items: any[], lower: number, upper: number, delivered = false) {
  if (!Array.isArray(items) || !items.length || !(lower > 0) || !(upper >= lower)) return items || [];
  return items.filter(item => { const price = itemPrice(item, delivered); return price > 0 && price >= lower && price <= upper; });
}
async function token() {
  if (ebayToken && Date.now() < ebayTokenExp - 60000) return ebayToken;
  const id = Deno.env.get("EBAY_CLIENT_ID"), sec = Deno.env.get("EBAY_CLIENT_SECRET");
  if (!id || !sec) throw Error("eBay credentials missing");
  const r = await fetch("https://api.ebay.com/identity/v1/oauth2/token", {
    method: "POST",
    headers: { Authorization: `Basic ${btoa(`${id}:${sec}`)}`, "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "client_credentials", scope: "https://api.ebay.com/oauth/api_scope" }),
  });
  if (!r.ok) throw Error(`eBay OAuth ${r.status}`);
  const d = await r.json();
  ebayToken = d.access_token;
  ebayTokenExp = Date.now() + Number(d.expires_in || 7200) * 1000;
  return ebayToken;
}
async function ebay(q: string, limit: number) {
  const u = new URL("https://api.ebay.com/buy/browse/v1/item_summary/search");
  u.searchParams.set("q", q);
  u.searchParams.set("limit", String(Math.min(limit, 40)));
  u.searchParams.set("filter", "itemLocationCountry:AU");
  const r = await fetch(u, { headers: { Authorization: `Bearer ${await token()}`, "X-EBAY-C-MARKETPLACE-ID": "EBAY_AU", "Accept-Language": "en-AU" } });
  if (!r.ok) throw Error(`eBay search ${r.status}`);
  const d = await r.json();
  const raw = (d.itemSummaries || []).map((i: any) => {
    const p = Number(i?.price?.value), vals = (i.shippingOptions || []).map((s: any) => Number(s?.shippingCost?.value)).filter((v: number) => Number.isFinite(v) && v >= 0), ship = vals.length ? Math.min(...vals) : 0;
    return { title: i.title || "", price: p, deliveredPrice: p + ship, condition: i.condition || "", url: i.itemWebUrl || null, seller: i.seller?.username || "eBay AU", source: "eBay AU", sourceClass: "used-marketplace" };
  }).filter((x: any) => Number.isFinite(x.price) && x.price > 0);
  const items = dedupeListings(raw);
  return { provider: "ebay", items, analysedListings: raw.length, retainedListings: items.length };
}
async function braveWeb(q: string, count: number) {
  const key = Deno.env.get("BRAVE_SEARCH_API_KEY");
  if (!key) throw Error("BRAVE_SEARCH_API_KEY missing");
  const u = new URL("https://api.search.brave.com/res/v1/web/search");
  u.searchParams.set("q", q); u.searchParams.set("country", "AU"); u.searchParams.set("search_lang", "en"); u.searchParams.set("ui_lang", "en-AU");
  u.searchParams.set("count", String(Math.min(count, 20))); u.searchParams.set("safesearch", "moderate");
  const r = await fetch(u, { headers: { Accept: "application/json", "X-Subscription-Token": key } });
  if (!r.ok) throw Error(`Brave Search ${r.status}`);
  return await r.json();
}
async function braveRetail(q: string, limit: number) {
  const d = await braveWeb(`${q} price Australia buy new`, limit);
  const raw = (d.web?.results || []).map((x: any) => {
    const text = `${x.title || ""} ${x.description || ""} ${(x.extra_snippets || []).join(" ")}`, price = priceFromText(text);
    let source = ""; try { source = new URL(x.url).hostname.replace(/^www\./, ""); } catch {}
    return { title: x.title || "", price, source: source || "Brave Search", store: source || "Brave Search", url: x.url || null, description: x.description || "", seller: "", sourceClass: "retail-reference" };
  }).filter((x: any) => x.price > 0);
  const items = dedupeListings(raw.filter((x: any) => isAllowedRetailResult(x)));
  return { provider: "brave", items, analysedListings: raw.length, retainedListings: items.length };
}
async function braveMarketplace(q: string, source: "gumtree" | "facebook", limit: number) {
  const site = source === "gumtree" ? "site:gumtree.com.au" : "site:facebook.com/marketplace/item";
  const d = await braveWeb(`${site} ${q}`, limit);
  const raw = (d.web?.results || []).map((x: any) => {
    const text = `${x.title || ""} ${x.description || ""}`, price = priceFromText(text);
    return { title: x.title || "", price, source: source === "gumtree" ? "Gumtree" : "Facebook Marketplace", url: x.url || "", condition: "Used / marketplace", snippet: x.description || "", sourceClass: "used-marketplace" };
  }).filter((x: any) => x.title && x.url && x.price > 0);
  return dedupeListings(raw.filter((x: any) => isAllowedMarketplaceResult(x, source))).slice(0, 20);
}
async function serpRetail(q: string, limit: number) {
  const key = Deno.env.get("SERPAPI_KEY"); if (!key) throw Error("SERPAPI_KEY missing");
  const u = new URL("https://serpapi.com/search.json");
  u.searchParams.set("engine", "google_shopping"); u.searchParams.set("q", q); u.searchParams.set("gl", "au"); u.searchParams.set("hl", "en"); u.searchParams.set("api_key", key);
  const r = await fetch(u); if (!r.ok) throw Error(`SerpApi ${r.status}`); const d = await r.json();
  const raw = (d.shopping_results || []).slice(0, limit).map((x: any) => ({ title: x.title || "", price: Number(x.extracted_price ?? String(x.price || "").replace(/[^0-9.]/g, "")), source: x.source || x.merchant || "Google Shopping", store: x.source || x.merchant || "Google Shopping", seller: x.merchant || x.source || "", url: x.product_link || x.link || null, sourceClass: "retail-reference" })).filter((x: any) => Number.isFinite(x.price) && x.price > 0);
  const items = dedupeListings(raw.filter((x: any) => isAllowedRetailResult(x)));
  return { provider: "serpapi-google-shopping", items, analysedListings: raw.length, retainedListings: items.length };
}
async function serpMarketplace(q: string, source: "gumtree" | "facebook") {
  const key = Deno.env.get("SERPAPI_KEY"); if (!key) throw Error("SERPAPI_KEY missing");
  const site = source === "gumtree" ? "site:gumtree.com.au" : "site:facebook.com/marketplace/item";
  const u = new URL("https://serpapi.com/search.json");
  u.searchParams.set("engine", "google"); u.searchParams.set("q", `${site} ${q}`); u.searchParams.set("gl", "au"); u.searchParams.set("hl", "en"); u.searchParams.set("num", "20"); u.searchParams.set("api_key", key);
  const r = await fetch(u); if (!r.ok) throw Error(`${source} fallback ${r.status}`); const d = await r.json();
  const raw = (d.organic_results || []).map((x: any) => {
    const text = `${x.title || ""} ${x.snippet || ""}`, price = priceFromText(text);
    return { title: x.title || "", price, source: source === "gumtree" ? "Gumtree" : "Facebook Marketplace", url: x.link || "", condition: "Used / marketplace", snippet: x.snippet || "", sourceClass: "used-marketplace" };
  }).filter((x: any) => x.title && x.url && x.price > 0);
  return dedupeListings(raw.filter((x: any) => isAllowedMarketplaceResult(x, source))).slice(0, 20);
}

Deno.serve(async req => {
  const h = cors(req), reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === "OPTIONS") return new Response("ok", { headers: h });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  try {
    const accessToken = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
    if (!accessToken) return reply({ error: "Authentication required" }, 401);
    const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, { global: { headers: { Authorization: `Bearer ${accessToken}` } }, auth: { persistSession: false } });
    const { data: { user }, error: userError } = await userClient.auth.getUser(accessToken);
    if (userError || !user) return reply({ error: "Invalid session" }, 401);

    const b = await req.json(), q = String(b.query || "").trim();
    if (!q) return reply({ error: "Enter a search query" }, 400);
    const limit = Math.min(Math.max(Number(b.limit || 30), 5), 40);
    const [er, br, gm, fm] = await Promise.allSettled([ebay(q, limit), braveRetail(q, limit), braveMarketplace(q, "gumtree", limit), braveMarketplace(q, "facebook", limit)]);
    let e = er.status === "fulfilled" ? er.value : null;
    let webRetail = br.status === "fulfilled" ? br.value : null, gumtree = gm.status === "fulfilled" ? gm.value : [], facebook = fm.status === "fulfilled" ? fm.value : [];
    let retailFallback = null, gumtreeFallback = null, facebookFallback = null;
    if (!webRetail || webRetail.items.length < 2) { try { webRetail = await serpRetail(q, limit); retailFallback = "serpapi-google-shopping"; } catch (err) { retailFallback = String(err); } }
    if (gumtree.length < 1) { try { gumtree = await serpMarketplace(q, "gumtree"); gumtreeFallback = "serpapi"; } catch (err) { gumtreeFallback = String(err); } }
    if (facebook.length < 1) { try { facebook = await serpMarketplace(q, "facebook"); facebookFallback = "serpapi"; } catch (err) { facebookFallback = String(err); } }
    if (!e && !webRetail && !gumtree.length && !facebook.length) throw Error("All market sources failed");

    const pricingAnalysis = analyseUsedMarket(e, gumtree, facebook);
    if (pricingAnalysis.rawListings >= 5 && pricingAnalysis.retainedListings > 0) {
      if (e) {
        e = { ...e, items: applyUsedBounds(e.items, pricingAnalysis.lowerBound, pricingAnalysis.upperBound, true) };
        e.retainedListings = e.items.length;
      }
      gumtree = applyUsedBounds(gumtree, pricingAnalysis.lowerBound, pricingAnalysis.upperBound);
      facebook = applyUsedBounds(facebook, pricingAnalysis.lowerBound, pricingAnalysis.upperBound);
    }

    return reply({
      success: true, query: q, currency: "AUD", ebay: e, webRetail, google: webRetail, retailProvider: webRetail?.provider || null,
      gumtree: { provider: gumtreeFallback ? "serpapi" : "brave", items: gumtree },
      facebook: { provider: facebookFallback ? "serpapi" : "brave", items: facebook },
      pricingAnalysis: { ...pricingAnalysis, method: "IQR outlier filter across retained Australian used-market listings", retailExcludedFromUsedMedian: true },
      sourcePriority: { used: ["ebay", "gumtree", "facebook"], retail: ["brave", "serpapi-google-shopping"], marketplaces: ["brave", "serpapi"] },
      sourcePolicy: { mode: "trusted-sellers-only", rejectsEditorial: true, rejectsReddit: true, retailIsReferenceOnly: true, outlierFilter: "IQR-1.5" },
      sourceErrors: { ebay: er.status === "rejected" ? String(er.reason) : null, braveRetail: br.status === "rejected" ? String(br.reason) : null, braveGumtree: gm.status === "rejected" ? String(gm.reason) : null, braveFacebook: fm.status === "rejected" ? String(fm.reason) : null, retailFallback, gumtreeFallback, facebookFallback },
    });
  } catch (err) {
    return reply({ error: err instanceof Error ? err.message : String(err) }, 500);
  }
});