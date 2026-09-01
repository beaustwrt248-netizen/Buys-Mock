import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const ORIGIN = "https://beaustwrt248-netizen.github.io";
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;
let ebayToken = "", ebayTokenExp = 0;

const n = (s: string) => String(s || "").toLowerCase().replace(/[^a-z0-9]+/g, " ").replace(/\s+/g, " ").trim();
function cors(_req: Request) {
  return {
    "Access-Control-Allow-Origin": ORIGIN,
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
  const items = (d.itemSummaries || []).map((i: any) => {
    const p = Number(i?.price?.value), vals = (i.shippingOptions || []).map((s: any) => Number(s?.shippingCost?.value)).filter((v: number) => Number.isFinite(v) && v >= 0), ship = vals.length ? Math.min(...vals) : 0;
    return { title: i.title || "", price: p, deliveredPrice: p + ship, condition: i.condition || "", url: i.itemWebUrl || null, seller: i.seller?.username || "eBay AU", source: "eBay AU" };
  }).filter((x: any) => Number.isFinite(x.price) && x.price > 0);
  return { provider: "ebay", items };
}
async function braveWeb(q: string, count: number) {
  const key = Deno.env.get("BRAVE_SEARCH_API_KEY");
  if (!key) throw Error("BRAVE_SEARCH_API_KEY missing");
  const u = new URL("https://api.search.brave.com/res/v1/web/search");
  u.searchParams.set("q", q);
  u.searchParams.set("country", "AU");
  u.searchParams.set("search_lang", "en");
  u.searchParams.set("ui_lang", "en-AU");
  u.searchParams.set("count", String(Math.min(count, 20)));
  u.searchParams.set("safesearch", "moderate");
  const r = await fetch(u, { headers: { Accept: "application/json", "X-Subscription-Token": key } });
  if (!r.ok) throw Error(`Brave Search ${r.status}`);
  return await r.json();
}
async function braveRetail(q: string, limit: number) {
  const d = await braveWeb(`${q} price Australia buy new`, limit);
  const items = (d.web?.results || []).map((x: any) => {
    const text = `${x.title || ""} ${x.description || ""} ${(x.extra_snippets || []).join(" ")}`, price = priceFromText(text);
    let source = "";
    try { source = new URL(x.url).hostname.replace(/^www\./, ""); } catch {}
    return { title: x.title || "", price, source: source || "Brave Search", store: source || "Brave Search", url: x.url || null, description: x.description || "" };
  }).filter((x: any) => x.price > 0);
  return { provider: "brave", items, analysedListings: items.length };
}
async function braveMarketplace(q: string, source: "gumtree" | "facebook", limit: number) {
  const site = source === "gumtree" ? "site:gumtree.com.au" : "site:facebook.com/marketplace/item";
  const d = await braveWeb(`${site} ${q}`, limit);
  const items = (d.web?.results || []).map((x: any) => {
    const text = `${x.title || ""} ${x.description || ""}`, price = priceFromText(text);
    return { title: x.title || "", price, source: source === "gumtree" ? "Gumtree" : "Facebook Marketplace", url: x.url || "", condition: "Used / marketplace", snippet: x.description || "" };
  }).filter((x: any) => x.title && x.url && x.price > 0);
  const seen = new Set<string>();
  return items.filter((x: any) => {
    const k = `${n(x.title)}|${Math.round(x.price)}|${x.url}`;
    if (seen.has(k)) return false;
    seen.add(k);
    return true;
  }).slice(0, 20);
}
async function serpRetail(q: string, limit: number) {
  const key = Deno.env.get("SERPAPI_KEY");
  if (!key) throw Error("SERPAPI_KEY missing");
  const u = new URL("https://serpapi.com/search.json");
  u.searchParams.set("engine", "google_shopping");
  u.searchParams.set("q", q);
  u.searchParams.set("gl", "au");
  u.searchParams.set("hl", "en");
  u.searchParams.set("api_key", key);
  const r = await fetch(u);
  if (!r.ok) throw Error(`SerpApi ${r.status}`);
  const d = await r.json();
  const items = (d.shopping_results || []).slice(0, limit).map((x: any) => ({ title: x.title || "", price: Number(x.extracted_price ?? String(x.price || "").replace(/[^0-9.]/g, "")), source: x.source || x.merchant || "Google Shopping", store: x.source || x.merchant || "Google Shopping", url: x.product_link || x.link || null })).filter((x: any) => Number.isFinite(x.price) && x.price > 0);
  return { provider: "serpapi-google-shopping", items, analysedListings: items.length };
}
async function serpMarketplace(q: string, source: "gumtree" | "facebook") {
  const key = Deno.env.get("SERPAPI_KEY");
  if (!key) throw Error("SERPAPI_KEY missing");
  const site = source === "gumtree" ? "site:gumtree.com.au" : "site:facebook.com/marketplace/item";
  const u = new URL("https://serpapi.com/search.json");
  u.searchParams.set("engine", "google");
  u.searchParams.set("q", `${site} ${q}`);
  u.searchParams.set("gl", "au");
  u.searchParams.set("hl", "en");
  u.searchParams.set("num", "20");
  u.searchParams.set("api_key", key);
  const r = await fetch(u);
  if (!r.ok) throw Error(`${source} fallback ${r.status}`);
  const d = await r.json();
  return (d.organic_results || []).map((x: any) => {
    const text = `${x.title || ""} ${x.snippet || ""}`, price = priceFromText(text);
    return { title: x.title || "", price, source: source === "gumtree" ? "Gumtree" : "Facebook Marketplace", url: x.link || "", condition: "Used / marketplace", snippet: x.snippet || "" };
  }).filter((x: any) => x.title && x.url && x.price > 0).slice(0, 20);
}

Deno.serve(async req => {
  const h = cors(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === "OPTIONS") return new Response("ok", { headers: h });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  try {
    const accessToken = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
    if (!accessToken) return reply({ error: "Authentication required" }, 401);
    const userClient = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
      global: { headers: { Authorization: `Bearer ${accessToken}` } },
      auth: { persistSession: false },
    });
    const { data: { user }, error: userError } = await userClient.auth.getUser(accessToken);
    if (userError || !user) return reply({ error: "Invalid session" }, 401);

    const b = await req.json(), q = String(b.query || "").trim();
    if (!q) return reply({ error: "Enter a search query" }, 400);
    const limit = Math.min(Math.max(Number(b.limit || 30), 5), 40);
    const [er, br, gm, fm] = await Promise.allSettled([ebay(q, limit), braveRetail(q, limit), braveMarketplace(q, "gumtree", limit), braveMarketplace(q, "facebook", limit)]);
    const e = er.status === "fulfilled" ? er.value : null;
    let webRetail = br.status === "fulfilled" ? br.value : null, gumtree = gm.status === "fulfilled" ? gm.value : [], facebook = fm.status === "fulfilled" ? fm.value : [];
    let retailFallback = null, gumtreeFallback = null, facebookFallback = null;
    if (!webRetail || webRetail.items.length < 2) { try { webRetail = await serpRetail(q, limit); retailFallback = "serpapi-google-shopping"; } catch (err) { retailFallback = String(err); } }
    if (gumtree.length < 1) { try { gumtree = await serpMarketplace(q, "gumtree"); gumtreeFallback = "serpapi"; } catch (err) { gumtreeFallback = String(err); } }
    if (facebook.length < 1) { try { facebook = await serpMarketplace(q, "facebook"); facebookFallback = "serpapi"; } catch (err) { facebookFallback = String(err); } }
    if (!e && !webRetail && !gumtree.length && !facebook.length) throw Error("All market sources failed");
    return reply({ success: true, query: q, currency: "AUD", ebay: e, webRetail, google: webRetail, retailProvider: webRetail?.provider || null, gumtree: { provider: gumtreeFallback ? "serpapi" : "brave", items: gumtree }, facebook: { provider: facebookFallback ? "serpapi" : "brave", items: facebook }, sourcePriority: { retail: ["brave", "serpapi-google-shopping"], marketplaces: ["brave", "serpapi"] }, sourceErrors: { ebay: er.status === "rejected" ? String(er.reason) : null, braveRetail: br.status === "rejected" ? String(br.reason) : null, braveGumtree: gm.status === "rejected" ? String(gm.reason) : null, braveFacebook: fm.status === "rejected" ? String(fm.reason) : null, retailFallback, gumtreeFallback, facebookFallback } });
  } catch (err) {
    return reply({ error: err instanceof Error ? err.message : String(err) }, 500);
  }
});
