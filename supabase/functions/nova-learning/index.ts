import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const U = Deno.env.get('SUPABASE_URL') || '';
const K = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const db = createClient(U, K, { auth: { persistSession: false, autoRefreshToken: false } });
const ORIGINS = new Set(['https://buyshub.me', 'https://www.buyshub.me', 'https://beaustwrt248-netizen.github.io']);
const PAGE = 1000;
const UPSERT_BATCH = 200;

const clean = (v: unknown, n = 1200) => String(v ?? '').trim().replace(/\s+/g, ' ').slice(0, n);
const days = (a: any, b: any) => {
  const x = new Date(String(a || '')).getTime(), y = new Date(String(b || '')).getTime();
  return Number.isFinite(x) && Number.isFinite(y) ? Math.max(0, Math.round((y - x) / 86400000)) : null;
};

function headers(r: Request) {
  const o = r.headers.get('Origin') || '';
  return {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': ORIGINS.has(o) ? o : 'https://buyshub.me',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Cache-Control': 'no-store',
    'Vary': 'Origin',
    'X-Content-Type-Options': 'nosniff'
  };
}

async function auth(r: Request) {
  if (!U || !K) return { error: 'Nova learning backend configuration unavailable', status: 503 } as const;
  const t = (r.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
  if (!t) return { error: 'Authentication required', status: 401 } as const;
  const { data: { user }, error } = await db.auth.getUser(t);
  if (error || !user) return { error: 'Invalid session', status: 401 } as const;
  const { data: p, error: e } = await db.from('profiles').select('role,is_enabled').eq('id', user.id).maybeSingle();
  if (e) throw e;
  if (!p?.is_enabled || p.role !== 'admin') return { error: 'Admin access required', status: 403 } as const;
  return { user } as const;
}

async function allRows(table: string, select: string, orderColumn: string, ascending = false, filter?: (q: any) => any) {
  const rows: any[] = [];
  for (let from = 0; ; from += PAGE) {
    let q: any = db.from(table).select(select).order(orderColumn, { ascending }).range(from, from + PAGE - 1);
    if (filter) q = filter(q);
    const { data, error } = await q;
    if (error) {
      if (error.code === '42P01') return rows;
      throw error;
    }
    const page = data || [];
    rows.push(...page);
    if (page.length < PAGE) return rows;
  }
}

function normaliseLesson(row: any) {
  return {
    ...row,
    summary: clean(row.summary),
    lesson_key: clean(row.lesson_key, 240),
    lesson_type: clean(row.lesson_type, 80),
    source_type: clean(row.source_type, 80),
    source_id: row.source_id ? clean(row.source_id, 160) : null,
    outcome: row.outcome ? clean(row.outcome, 120) : null,
    updated_at: new Date().toISOString()
  };
}

async function upsertLessons(rows: any[]) {
  let written = 0;
  for (let i = 0; i < rows.length; i += UPSERT_BATCH) {
    const batch = rows.slice(i, i + UPSERT_BATCH).map(normaliseLesson);
    const { error } = await db.from('nova_learning_experiences').upsert(batch, { onConflict: 'domain,lesson_key' });
    if (error) throw error;
    written += batch.length;
  }
  return written;
}

async function harvest(uid: string) {
  const [incidents, repairs, vals, inv, sales] = await Promise.all([
    allRows('guardian_incidents', 'id,fingerprint,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,resolution_summary,occurrence_count,verified_at,last_seen_at,updated_at', 'updated_at'),
    allRows('guardian_repairs', 'id,incident_id,status,patch_summary,test_results,tested_at,completed_at,updated_at', 'updated_at'),
    allRows('valuation_history', 'id,item_type,item_summary,item_grade,status,expected_profit,actual_profit,bought_price,sold_price,created_at,updated_at', 'created_at'),
    allRows('inventory_items', 'id,valuation_id,device_catalog_id,item_type,item_summary,model_number,storage,item_grade,acquired_price,expected_sale_price,status,acquired_at,listed_at,updated_at', 'acquired_at'),
    allRows('sales_records', 'id,inventory_item_id,valuation_id,acquired_cost,sold_price,fees,other_costs,realised_profit,sales_channel,sold_at,created_at', 'sold_at')
  ]);

  const lessons: any[] = [];
  const invBy = new Map(inv.map((x: any) => [String(x.id), x]));

  for (const i of incidents) {
    const resolved = !!i.verified_at || String(i.state).toLowerCase() === 'resolved';
    const recurring = Number(i.occurrence_count || 1) > 1;
    if (!resolved && !recurring) continue;
    lessons.push({
      domain: 'guardian', lesson_key: `incident:${i.fingerprint || i.id}:${resolved ? 'verified' : 'recurring'}`,
      lesson_type: resolved ? 'verified_outcome' : 'recurring_pattern',
      summary: resolved
        ? `${i.classification || 'Guardian incident'} resolved/verified after ${i.occurrence_count || 1} occurrence(s). Diagnosis: ${i.diagnosis_summary || 'not recorded'}. Resolution: ${i.resolution_summary || 'not recorded'}.`
        : `${i.classification || 'Guardian incident'} has recurred ${i.occurrence_count || 1} times. Diagnosis: ${i.diagnosis_summary || 'not recorded'}.`,
      source_type: 'guardian_incident', source_id: String(i.id),
      evidence: { risk_level: i.risk_level, classification: i.classification, diagnosis_summary: i.diagnosis_summary, proposed_action: i.proposed_action, resolution_summary: i.resolution_summary, occurrence_count: i.occurrence_count },
      outcome: resolved ? 'resolved' : 'recurring', confidence: Number(i.confidence ?? (resolved ? .9 : .7)), verified: resolved, active: true,
      observed_at: i.last_seen_at || i.updated_at, created_by: uid
    });
  }

  for (const r of repairs) {
    const s = String(r.status || '').toLowerCase();
    const ok = ['completed', 'verified', 'merged', 'applied'].includes(s) || !!r.completed_at;
    const bad = ['failed', 'rejected', 'quarantined'].includes(s);
    if (!ok && !bad) continue;
    lessons.push({
      domain: 'guardian', lesson_key: `repair:${r.id}:${ok ? 'success' : 'failure'}`,
      lesson_type: ok ? 'repair_success' : 'repair_failure', summary: `Guardian repair ${ok ? 'succeeded' : 'failed'}${r.patch_summary ? `: ${r.patch_summary}` : ''}.`,
      source_type: 'guardian_repair', source_id: String(r.id), evidence: { incident_id: r.incident_id, status: r.status, patch_summary: r.patch_summary, test_results: r.test_results },
      outcome: ok ? 'success' : 'failure', confidence: ok ? .95 : .9, verified: ok, active: true, observed_at: r.completed_at || r.tested_at || r.updated_at, created_by: uid
    });
  }

  for (const x of inv) {
    if (String(x.status).toLowerCase() === 'sold') continue;
    const held = days(x.acquired_at, new Date());
    lessons.push({
      domain: 'inventory', lesson_key: `inventory:${x.id}:holding`, lesson_type: 'authoritative_holding',
      summary: `${x.item_summary || x.item_type || 'Item'} is ${x.status || 'in inventory'} after acquisition at AUD ${Number(x.acquired_price || 0).toFixed(2)}${held == null ? '' : ` and has been held about ${held} day(s)`}.`,
      source_type: 'inventory_items', source_id: String(x.id),
      evidence: { valuation_id: x.valuation_id, device_catalog_id: x.device_catalog_id, item_type: x.item_type, model_number: x.model_number, storage: x.storage, item_grade: x.item_grade, acquired_price: x.acquired_price, expected_sale_price: x.expected_sale_price, status: x.status, holding_days: held },
      outcome: String(x.status || 'holding'), confidence: .99, verified: true, active: true, observed_at: x.updated_at || x.acquired_at, created_by: uid
    });
  }

  for (const s of sales) {
    const x: any = invBy.get(String(s.inventory_item_id)) || null;
    const held = x ? days(x.acquired_at, s.sold_at) : null;
    const profit = Number(s.realised_profit ?? (Number(s.sold_price || 0) - Number(s.acquired_cost || 0) - Number(s.fees || 0) - Number(s.other_costs || 0)));
    lessons.push({
      domain: 'sales', lesson_key: `sale:${s.id}:authoritative`, lesson_type: 'authoritative_realised_sale',
      summary: `${x?.item_summary || 'Inventory item'} sold for AUD ${Number(s.sold_price || 0).toFixed(2)} with realised profit AUD ${profit.toFixed(2)}${held == null ? '' : ` after about ${held} day(s) held`}.`,
      source_type: 'sales_records', source_id: String(s.id),
      evidence: { inventory_item_id: s.inventory_item_id, item_type: x?.item_type, item_grade: x?.item_grade, model_number: x?.model_number, storage: x?.storage, acquired_cost: s.acquired_cost, sold_price: s.sold_price, fees: s.fees, other_costs: s.other_costs, realised_profit: profit, sales_channel: s.sales_channel, holding_days: held },
      outcome: profit >= 0 ? 'positive_realised_profit' : 'negative_realised_profit', confidence: .99, verified: true, active: true, observed_at: s.sold_at || s.created_at, created_by: uid
    });
  }

  for (const v of vals) {
    if (v.actual_profit == null || v.expected_profit == null) continue;
    const expected = Number(v.expected_profit), actual = Number(v.actual_profit), diff = actual - expected;
    lessons.push({
      domain: 'valuation', lesson_key: `valuation:${v.id}:realised`, lesson_type: 'realised_profit',
      summary: `${v.item_summary || v.item_type || 'Valuation'} realised profit AUD ${actual.toFixed(2)} versus expected AUD ${expected.toFixed(2)} (${diff >= 0 ? '+' : ''}${diff.toFixed(2)} variance).`,
      source_type: 'valuation_history', source_id: String(v.id), evidence: { item_type: v.item_type, item_grade: v.item_grade, status: v.status, expected_profit: expected, actual_profit: actual, bought_price: v.bought_price, sold_price: v.sold_price, forecast_error: diff },
      outcome: diff >= 0 ? 'outperformed' : 'underperformed', confidence: .95, verified: true, active: true, observed_at: v.updated_at || v.created_at, created_by: uid
    });
  }

  const written = await upsertLessons(lessons);
  return { written, incident_sample: incidents.length, repair_sample: repairs.length, valuation_sample: vals.length, inventory_sample: inv.length, sales_sample: sales.length, complete_scan: true };
}

async function summary() {
  const rows = await allRows('nova_learning_experiences', 'id,domain,lesson_type,summary,source_type,source_id,outcome,confidence,verified,observed_at,created_at', 'observed_at', false, q => q.eq('active', true));
  const by_domain: Record<string, number> = {}, by_type: Record<string, number> = {};
  for (const r of rows) {
    by_domain[r.domain] = (by_domain[r.domain] || 0) + 1;
    by_type[r.lesson_type] = (by_type[r.lesson_type] || 0) + 1;
  }
  return {
    count: rows.length,
    verified_count: rows.filter(r => r.verified).length,
    unverified_count: rows.filter(r => !r.verified).length,
    low_confidence_count: rows.filter(r => Number(r.confidence || 0) < .7).length,
    by_domain,
    by_type,
    recent: rows.slice(0, 20),
    access: 'admin-only',
    complete_scan: true
  };
}

Deno.serve(async (req: Request) => {
  const h = headers(req), reply = (b: unknown, s = 200) => new Response(JSON.stringify(b), { status: s, headers: h });
  if (req.method === 'OPTIONS') return new Response('ok', { headers: h });
  if (req.method !== 'POST') return reply({ error: 'POST required' }, 405);
  try {
    const a = await auth(req);
    if ('error' in a) return reply({ error: a.error }, a.status);
    let b: any = {};
    try { b = await req.json(); } catch { return reply({ error: 'Invalid JSON request' }, 400); }
    const action = clean(b.action, 40).toLowerCase();
    if (action === 'summary') return reply({ ok: true, ...await summary() });
    if (action === 'harvest') return reply({ ok: true, ...await harvest(a.user.id) });
    if (action === 'feedback') {
      const domain = clean(b.domain, 40), text = clean(b.summary);
      const allowed = new Set(['guardian', 'valuation', 'catalogue', 'inventory', 'sales', 'support', 'release', 'pricing', 'admin']);
      if (!allowed.has(domain) || !text) return reply({ error: 'Valid domain and feedback summary required' }, 400);
      const key = `feedback:${a.user.id}:${crypto.randomUUID()}`;
      await upsertLessons([{ domain, lesson_key: key, lesson_type: 'human_feedback', summary: text, source_type: 'admin_feedback', source_id: String(a.user.id), evidence: { note: 'Explicit Admin feedback; non-authoritative until corroborated by source data.' }, outcome: 'feedback', confidence: .6, verified: false, active: true, observed_at: new Date().toISOString(), created_by: a.user.id }]);
      return reply({ ok: true, lesson_key: key });
    }
    return reply({ error: 'Unsupported action' }, 400);
  } catch (e) {
    const m = e instanceof Error ? e.message : clean(e, 500);
    console.error(`[nova-learning] ${m}`);
    return new Response(JSON.stringify({ error: m }), { status: 500, headers: headers(req) });
  }
});
