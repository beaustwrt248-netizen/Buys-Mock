import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ORIGINS = new Set(['https://buyshub.me','https://www.buyshub.me','https://beaustwrt248-netizen.github.io']);
const clean = (v: unknown, n = 300) => String(v ?? '').trim().slice(0, n);

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
async function authorisedAdmin(req: Request) {
  const token = (req.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
  if (!token) return null;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return null;
  const { data: profile } = await admin.from('profiles').select('role,is_enabled').eq('id', user.id).maybeSingle();
  return profile?.is_enabled && profile.role === 'admin' ? user : null;
}
function inc(map: Record<string, number>, key: string) { map[key] = (map[key] || 0) + 1; }

Deno.serve(async req => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === 'OPTIONS') return new Response('ok', { headers: h });
  if (req.method !== 'POST') return reply({ error: 'POST required' }, 405);
  try {
    const user = await authorisedAdmin(req);
    if (!user) return reply({ error: 'Admin access required' }, 403);

    const [catalogueResult, queueResult, findingResult] = await Promise.all([
      admin.from('device_catalog').select('id,brand,category,model_number,storage_options,ram_options,market_region,image_url').eq('active', true).limit(5000),
      admin.from('nova_catalog_audit_queue').select('id,status,priority,device_id').limit(5000),
      admin.from('nova_catalog_audit_findings').select('id,status,severity,field_name,requires_approval,device_id').limit(5000)
    ]);
    if (catalogueResult.error || queueResult.error || findingResult.error) return reply({ error: 'Catalogue intelligence unavailable' }, 503);

    const catalogue = catalogueResult.data || [];
    const queue = queueResult.data || [];
    const findings = findingResult.data || [];
    let missingModel = 0, missingStorage = 0, missingRam = 0, imageGaps = 0, marketRegionGaps = 0;
    const categories: Record<string, number> = {};
    const brands: Record<string, number> = {};
    const requiresStorage = new Set(['mobile_phone','tablet','laptop','desktop','console']);
    for (const row of catalogue) {
      const category = clean(row.category || 'unknown', 80).toLowerCase();
      const brand = clean(row.brand || 'unknown', 80);
      inc(categories, category || 'unknown');
      inc(brands, brand || 'unknown');
      if (!clean(row.model_number, 120)) missingModel++;
      if (requiresStorage.has(category) && (!Array.isArray(row.storage_options) || row.storage_options.length === 0)) missingStorage++;
      if (category === 'mobile_phone' && brand.toLowerCase() !== 'apple' && (!Array.isArray(row.ram_options) || row.ram_options.length === 0)) missingRam++;
      if (!clean(row.image_url, 500)) imageGaps++;
      if (!clean(row.market_region, 80)) marketRegionGaps++;
    }

    const queueByStatus: Record<string, number> = {}, queueByPriority: Record<string, number> = {};
    for (const row of queue) { inc(queueByStatus, clean(row.status || 'unknown', 40)); inc(queueByPriority, clean(row.priority || 'unknown', 40)); }
    const findingByStatus: Record<string, number> = {}, findingByField: Record<string, number> = {}, findingBySeverity: Record<string, number> = {};
    let approvals = 0;
    for (const row of findings) {
      inc(findingByStatus, clean(row.status || 'unknown', 40));
      inc(findingByField, clean(row.field_name || 'unknown', 80));
      inc(findingBySeverity, clean(row.severity || 'unknown', 40));
      if (row.requires_approval && !['applied','superseded','rejected','closed'].includes(clean(row.status, 40).toLowerCase())) approvals++;
    }

    return reply({
      ok: true,
      catalogue: {
        active_devices: catalogue.length,
        missing_model_number: missingModel,
        actionable_storage_gaps: missingStorage,
        non_apple_phone_ram_gaps: missingRam,
        image_gaps: imageGaps,
        market_region_gaps: marketRegionGaps,
        categories,
        top_brands: Object.fromEntries(Object.entries(brands).sort((a,b) => b[1] - a[1]).slice(0, 20))
      },
      audit_queue: { total: queue.length, by_status: queueByStatus, by_priority: queueByPriority },
      findings: { total: findings.length, awaiting_human_approval: approvals, by_status: findingByStatus, by_field: findingByField, by_severity: findingBySeverity },
      authority: 'Nova may identify gaps and queue evidence-backed findings; applying protected changes remains subject to the existing approval controls.'
    });
  } catch (error) {
    console.error('[nova-catalogue-intelligence]', clean(error instanceof Error ? error.message : error));
    return reply({ error: 'Catalogue intelligence could not complete this request' }, 500);
  }
});
