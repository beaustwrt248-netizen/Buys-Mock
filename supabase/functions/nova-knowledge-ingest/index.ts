import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import { MAX_INGESTION_BATCH, SAFE_INGESTION_ADAPTERS, runInternalIngestion } from "../_shared/nova_internal_ingestion.mjs";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"]);
const ADAPTERS = new Set(SAFE_INGESTION_ADAPTERS);
const clean = (value: unknown, max = 220) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

function headers(req: Request) {
  const origin = req.headers.get("Origin") || "";
  return {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": ORIGINS.has(origin) ? origin : "https://buyshub.me",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Cache-Control": "no-store",
    "Vary": "Origin",
    "X-Content-Type-Options": "nosniff",
  };
}

async function auth(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE) return { error: "Nova ingestion backend configuration unavailable", status: 503 } as const;
  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) return { error: "Authentication required", status: 401 } as const;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return { error: "Invalid session", status: 401 } as const;
  const { data: p, error: profileError } = await admin.from("profiles").select("role,is_enabled").eq("id", user.id).maybeSingle();
  if (profileError) throw profileError;
  if (!p?.is_enabled || p.role !== "admin") return { error: "Admin access required", status: 403 } as const;
  return { user } as const;
}

Deno.serve(async (req: Request) => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === "OPTIONS") return new Response("ok", { headers: h });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);

  const authorized = await auth(req);
  if ("error" in authorized) return reply({ error: authorized.error }, authorized.status);

  let body: any = {};
  try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
  if (clean(body.action, 40).toLowerCase() !== "ingest_internal") return reply({ error: "Unsupported action" }, 400);
  const adapter = clean(body.adapter, 40).toLowerCase() || "all";
  if (!ADAPTERS.has(adapter)) return reply({ error: "Invalid ingestion adapter" }, 400);
  const limit = bounded(body.limit, 20, MAX_INGESTION_BATCH);
  const offset = Math.max(Number(body.offset) || 0, 0);

  try {
    const result = await runInternalIngestion({ admin, adapter, limit, offset, actorId: authorized.user.id });
    return reply(result);
  } catch (error) {
    const message = clean(error instanceof Error ? error.message : error, 500);
    console.error(`[nova-knowledge-ingest] ${message}`);
    return reply({ error: message }, 500);
  }
});