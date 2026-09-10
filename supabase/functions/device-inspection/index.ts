import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const OPENAI_API_KEY = Deno.env.get("OPENAI_API_KEY") || "";
const MODEL = Deno.env.get("DEVICE_INSPECTION_MODEL") || Deno.env.get("NOVA_VISION_MODEL") || "gpt-5.6-sol";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, {
  auth: { persistSession: false, autoRefreshToken: false },
});

const ORIGINS = new Set([
  "https://buyshub.me",
  "https://www.buyshub.me",
  "https://beaustwrt248-netizen.github.io",
]);
const REQUIRED_IMAGES = 2;
const MAX_SINGLE_DATA_URL = 8_000_000;
const MAX_TOTAL_DATA_URL = 20_000_000;

const clean = (value: unknown, max = 500) =>
  String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);

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

async function authorise(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE) {
    return { error: "Device inspection configuration unavailable", status: 503 } as const;
  }
  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) return { error: "Authentication required", status: 401 } as const;

  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return { error: "Invalid session", status: 401 } as const;

  const { data: profile, error: profileError } = await admin
    .from("profiles")
    .select("is_enabled")
    .eq("id", user.id)
    .maybeSingle();
  if (profileError) throw profileError;
  if (!profile?.is_enabled) return { error: "Authorised Morley account required", status: 403 } as const;
  return { user } as const;
}

function collectImages(body: any) {
  const supplied = Array.isArray(body?.image_data_urls) ? body.image_data_urls : [];
  const images = supplied.map((value: unknown) => String(value || "")).filter(Boolean);
  if (images.length !== REQUIRED_IMAGES) throw new Error("TWO_IMAGES_REQUIRED");
  let total = 0;
  for (const image of images) {
    if (!/^data:image\/(jpeg|jpg|png|webp);base64,/i.test(image)) throw new Error("IMAGE_TYPE");
    if (image.length > MAX_SINGLE_DATA_URL) throw new Error("IMAGE_SIZE");
    total += image.length;
  }
  if (total > MAX_TOTAL_DATA_URL) throw new Error("IMAGE_TOTAL_SIZE");
  return images;
}

function responseText(data: any) {
  for (const item of data?.output || []) {
    for (const part of item?.content || []) {
      if (typeof part?.text === "string") return part.text;
    }
  }
  return "";
}

function parseJson(text: string) {
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start < 0 || end <= start) throw new Error("Vision model returned no structured result");
  return JSON.parse(text.slice(start, end + 1));
}

function clamp01(value: unknown) {
  const n = Number(value);
  return Number.isFinite(n) ? Math.max(0, Math.min(1, n)) : 0;
}

function textArray(value: any, limit: number, max: number) {
  return Array.isArray(value)
    ? value.map((item: unknown) => clean(item, max)).filter(Boolean).slice(0, limit)
    : [];
}

function maskEvidence(value: unknown, max = 240) {
  let text = clean(value, max);
  text = text.replace(
    /\b(IMEI(?:\s*\d)?|serial(?:\s*(?:number|no\.?))?)\s*[:#-]?\s*([A-Za-z0-9-]{6,})/gi,
    (_match, label, identifier) => `${label} ••••${String(identifier).replace(/-/g, "").slice(-4)}`,
  );
  text = text.replace(/\b\d{10,20}\b/g, value => `••••${value.slice(-4)}`);
  return text;
}

function evidenceArray(value: any, limit: number, max: number) {
  return Array.isArray(value)
    ? value.map((item: unknown) => maskEvidence(item, max)).filter(Boolean).slice(0, limit)
    : [];
}

function normaliseGrade(value: unknown) {
  const grade = clean(value, 16).toUpperCase();
  return ["A", "B", "C", "D", "PARTS"].includes(grade) ? grade : null;
}

function normaliseDamageRegions(value: any) {
  if (!Array.isArray(value)) return [];
  const regions: any[] = [];
  for (const item of value) {
    const photoIndex = Number(item?.photo_index);
    if (!Number.isInteger(photoIndex) || photoIndex < 1 || photoIndex > REQUIRED_IMAGES) continue;

    const x = clamp01(item?.x);
    const y = clamp01(item?.y);
    const width = Math.max(0.02, clamp01(item?.width));
    const height = Math.max(0.02, clamp01(item?.height));
    if (x >= 1 || y >= 1) continue;

    regions.push({
      photo_index: photoIndex,
      label: clean(item?.label, 100) || "Visible damage",
      severity: ["minor", "moderate", "major"].includes(clean(item?.severity, 20).toLowerCase())
        ? clean(item?.severity, 20).toLowerCase()
        : "minor",
      confidence: clamp01(item?.confidence),
      x,
      y,
      width: Math.min(width, 1 - x),
      height: Math.min(height, 1 - y),
    });
    if (regions.length >= 12) break;
  }
  return regions;
}

async function catalogueMatch(parsed: any) {
  const fields =
    "id,category,brand,family,model_name,model_number,release_year,storage_options,market_region,image_reference_url";

  const modelNumber = clean(parsed?.model_number, 120);
  if (modelNumber) {
    const { data, error } = await admin
      .from("device_catalog")
      .select(fields)
      .eq("active", true)
      .ilike("model_number", modelNumber)
      .limit(1);
    if (error) throw error;
    if (data?.length) return data[0];
  }

  const model = clean(parsed?.likely_model, 160);
  const brand = clean(parsed?.likely_brand, 100);
  if (model) {
    let query = admin
      .from("device_catalog")
      .select(fields)
      .eq("active", true)
      .ilike("model_name", `%${model.replace(/[%_]/g, " ")}%`);
    if (brand) query = query.ilike("brand", `%${brand.replace(/[%_]/g, " ")}%`);
    const { data, error } = await query.limit(1);
    if (error) throw error;
    if (data?.length) return data[0];
  }
  return null;
}

Deno.serve(async (req: Request) => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) =>
    new Response(JSON.stringify(body), { status, headers: h });

  if (req.method === "OPTIONS") return new Response("ok", { headers: h });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);

  try {
    const auth = await authorise(req);
    if ("error" in auth) return reply({ error: auth.error }, auth.status);
    if (!OPENAI_API_KEY) return reply({ error: "Device inspection provider is not configured" }, 503);

    let body: any = {};
    try {
      body = await req.json();
    } catch {
      return reply({ error: "Invalid JSON request" }, 400);
    }

    let images: string[];
    try {
      images = collectImages(body);
    } catch (error) {
      const message = String(error instanceof Error ? error.message : error);
      if (message === "TWO_IMAGES_REQUIRED") {
        return reply({ error: "Take exactly two photos: front first, then back." }, 400);
      }
      if (message === "IMAGE_TYPE") return reply({ error: "JPEG, PNG or WebP images are required." }, 400);
      if (message === "IMAGE_SIZE") return reply({ error: "Each image must be under about 6 MB." }, 413);
      return reply({ error: "Combined image payload is too large." }, 413);
    }

    const prompt = `You are Morley Buys Device Inspection in Australia. Inspect EXACTLY TWO photos of the same trade-in device. Photo 1 is the FRONT. Photo 2 is the BACK. Identify the device conservatively and assess visible physical condition only.

Critical damage-location task: when you can clearly see a crack, deep scratch, chip, dent, broken glass, missing physical piece, bent area, or other visible damage, return one damage_regions object for each distinct area. Coordinates MUST be normalized 0..1 relative to the full displayed photo: x and y are the top-left of a tight bounding ellipse/box, width and height are its size. Only mark a region when the visible evidence is strong enough that a red circle over that region would help a staff member find the actual damage. Do not create a region for reflections, glare, fingerprints, shadows, dust, screen content, wallpaper, or uncertain marks. If the location is uncertain, mention it only in uncertainties, not damage_regions.

Never invent storage, model number, damage, serial, IMEI, accessories, or hidden functionality. Never return full serial/IMEI values. Condition grade is advisory only: A = excellent/minimal visible wear; B = good/light wear; C = fair/clear cosmetic wear; D = poor/significant visible damage; PARTS = appears unsuitable except for parts. Do not infer whether cameras, buttons, charging, battery, screen touch, speakers, biometrics, ports, water resistance or radios work from photos.

Return JSON only with these keys:
likely_brand, likely_family, likely_model, model_number, colour, storage,
condition_grade, condition_summary, confidence,
damage_flags (string array),
damage_regions (array of objects: photo_index, label, severity minor|moderate|major, confidence, x, y, width, height),
evidence (string array), uncertainties (string array), next_photos (string array), catalogue_search_terms (string array).

Photo 1 is FRONT; Photo 2 is BACK.`;

    const content: any[] = [
      { type: "input_text", text: prompt },
      ...images.map(image_url => ({ type: "input_image", image_url, detail: "high" })),
    ];
    const payload = {
      model: MODEL,
      reasoning: { effort: "medium" },
      max_output_tokens: 2400,
      input: [{ role: "user", content }],
    };

    const provider = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${OPENAI_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(payload),
    });

    if (!provider.ok) {
      const detail = clean(await provider.text(), 500);
      console.error(`[device-inspection] provider ${provider.status}${detail ? `: ${detail}` : ""}`);
      return reply({ error: "Device inspection is temporarily unavailable" }, 502);
    }

    const parsed = parseJson(responseText(await provider.json()));
    const match = await catalogueMatch(parsed);
    const result = {
      likely_brand: clean(parsed.likely_brand, 100) || null,
      likely_family: clean(parsed.likely_family, 140) || null,
      likely_model: clean(parsed.likely_model, 180) || null,
      model_number: clean(parsed.model_number, 120) || null,
      colour: clean(parsed.colour, 100) || null,
      storage: clean(parsed.storage, 100) || null,
      condition_grade: normaliseGrade(parsed.condition_grade),
      condition_summary: clean(parsed.condition_summary, 320) || null,
      confidence: clamp01(parsed.confidence),
      damage_flags: textArray(parsed.damage_flags, 12, 180),
      damage_regions: normaliseDamageRegions(parsed.damage_regions),
      evidence: evidenceArray(parsed.evidence, 16, 240),
      uncertainties: textArray(parsed.uncertainties, 16, 240),
      next_photos: textArray(parsed.next_photos, 6, 180),
      catalogue_search_terms: textArray(parsed.catalogue_search_terms, 8, 140),
    };

    return reply({
      ok: true,
      checked_at: new Date().toISOString(),
      model: MODEL,
      photo_count: REQUIRED_IMAGES,
      capture_order: ["front", "back"],
      privacy: {
        stored: false,
        full_serials_returned: false,
      },
      result,
      catalogue_match: match,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : "Device inspection error";
    console.error(`[device-inspection] ${message}`);
    return new Response(
      JSON.stringify({ error: "Device inspection could not complete this request" }),
      { status: 500, headers: headers(req) },
    );
  }
});
