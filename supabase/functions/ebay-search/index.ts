import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { toLegacyMarketResponse } from "./compat.mjs";

const ALLOWED_ORIGINS = new Set([
  "https://buyshub.me",
  "https://www.buyshub.me",
  "https://beaustwrt248-netizen.github.io",
]);
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SUPABASE_ANON_KEY = Deno.env.get("SUPABASE_ANON_KEY")!;

function cors(req: Request) {
  const origin = req.headers.get("origin") || "";
  const allowedOrigin = ALLOWED_ORIGINS.has(origin) ? origin : "https://buyshub.me";
  return {
    "Access-Control-Allow-Origin": allowedOrigin,
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Content-Type": "application/json",
    "Cache-Control": "no-store",
    "Vary": "Origin",
    "X-Content-Type-Options": "nosniff",
  };
}

Deno.serve(async (req: Request) => {
  const headers = cors(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers });

  if (req.method === "OPTIONS") return new Response("ok", { headers });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);

  try {
    const authorization = req.headers.get("authorization") || "";
    if (!/^Bearer\s+/i.test(authorization)) return reply({ error: "Authentication required" }, 401);

    const body = await req.json();
    const query = String(body?.query || "").trim();
    if (!query) return reply({ error: "Enter a search query" }, 400);

    const limit = Math.min(Math.max(Number(body?.limit || 30), 5), 40);
    const upstream = await fetch(`${SUPABASE_URL}/functions/v1/market-search-v2`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": authorization,
        "apikey": req.headers.get("apikey") || SUPABASE_ANON_KEY,
      },
      body: JSON.stringify({ query, limit }),
    });

    const text = await upstream.text();
    let payload: any;
    try { payload = JSON.parse(text); } catch { payload = { error: text || `market-search-v2 HTTP ${upstream.status}` }; }
    if (!upstream.ok || !payload?.success) {
      return reply({ error: payload?.error || `market-search-v2 HTTP ${upstream.status}` }, upstream.status || 502);
    }

    return reply(toLegacyMarketResponse(payload, query));
  } catch (error) {
    return reply({ error: error instanceof Error ? error.message : String(error) }, 500);
  }
});
