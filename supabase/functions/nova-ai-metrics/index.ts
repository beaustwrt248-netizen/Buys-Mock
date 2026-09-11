import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ORIGINS = new Set(['https://buyshub.me','https://www.buyshub.me','https://beaustwrt248-netizen.github.io']);

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

Deno.serve(async (req: Request) => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === 'OPTIONS') return new Response('ok', { headers: h });
  if (req.method !== 'POST') return reply({ error: 'POST required' }, 405);
  const user = await authorisedAdmin(req);
  if (!user) return reply({ error: 'Admin access required' }, 403);

  const { data, error } = await admin.from('nova_ai_runs')
    .select('created_at,provider_mode,result_mode,models_used,latency_ms,input_tokens,output_tokens,cost_usd,success,degraded,failure_code,failure_count')
    .order('created_at', { ascending: false })
    .limit(1000);
  if (error) return reply({ error: 'Telemetry unavailable' }, 503);

  const rows = data || [];
  let success = 0, degraded = 0, latency = 0, input = 0, output = 0, cost = 0;
  const models: Record<string, number> = {};
  const failures: Record<string, number> = {};
  for (const row of rows) {
    if (row.success) success++;
    if (row.degraded) degraded++;
    latency += Number(row.latency_ms || 0);
    input += Number(row.input_tokens || 0);
    output += Number(row.output_tokens || 0);
    cost += Number(row.cost_usd || 0);
    for (const model of row.models_used || []) models[model] = (models[model] || 0) + 1;
    if (row.failure_code) failures[row.failure_code] = (failures[row.failure_code] || 0) + 1;
  }

  return reply({
    ok: true,
    total_runs: rows.length,
    success_runs: success,
    success_rate: rows.length ? Number((success / rows.length).toFixed(4)) : 0,
    degraded_runs: degraded,
    average_latency_ms: rows.length ? Math.round(latency / rows.length) : 0,
    input_tokens: input,
    output_tokens: output,
    cost_usd: Number(cost.toFixed(6)),
    model_runs: models,
    failure_codes: failures,
    last_run_at: rows[0]?.created_at || null,
    retention_note: 'Metrics contain operational metadata only; prompts and model responses are not stored.'
  });
});
