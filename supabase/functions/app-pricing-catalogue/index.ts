import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const url = Deno.env.get("SUPABASE_URL") || "";
const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const admin = createClient(url, service, {
  auth: { persistSession: false, autoRefreshToken: false },
});
const headers = {
  "Content-Type": "application/json",
  "Cache-Control": "no-store",
  "X-Content-Type-Options": "nosniff",
};

Deno.serve(async (req: Request) => {
  const reply = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers });

  if (req.method !== "GET" && req.method !== "POST") {
    return reply({ error: "GET or POST required" }, 405);
  }
  if (!url || !service) return reply({ error: "Pricing service unavailable" }, 503);

  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) return reply({ error: "Authentication required" }, 401);

  const {
    data: { user },
    error: userError,
  } = await admin.auth.getUser(token);
  if (userError || !user) return reply({ error: "Invalid session" }, 401);

  const { data: profile, error: profileError } = await admin
    .from("profiles")
    .select("is_enabled")
    .eq("id", user.id)
    .maybeSingle();
  if (profileError || !profile?.is_enabled) {
    return reply({ error: "Account unavailable" }, 403);
  }

  const [priceResult, deviceResult] = await Promise.all([
    admin
      .from("device_buy_prices")
      .select("device_catalog_id,storage,price_aud,authoritative")
      .eq("is_active", true)
      .eq("authoritative", true)
      .eq("condition_grade", "base")
      .not("price_aud", "is", null)
      .limit(5000),
    admin
      .from("device_catalog")
      .select(
        "id,category,brand,family,model_name,model_number,release_year,storage_options,ram_options,image_reference_url,market_region",
      )
      .eq("active", true)
      .order("category", { ascending: true })
      .order("brand", { ascending: true })
      .order("model_name", { ascending: true })
      .limit(5000),
  ]);

  if (priceResult.error) return reply({ error: "Pricing catalogue unavailable" }, 500);
  if (deviceResult.error) return reply({ error: "Device catalogue unavailable" }, 500);

  const devices = deviceResult.data || [];
  const byId = new Map(devices.map((device: any) => [device.id, device]));
  const prices = (priceResult.data || []).map((price: any) => ({
    ...price,
    device: byId.get(price.device_catalog_id) || null,
  }));

  return reply({ devices, prices });
});
