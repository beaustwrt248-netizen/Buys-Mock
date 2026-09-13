import {
  chunkKnowledgeDocument,
  normalizeKnowledgeDocument,
  sanitizeMetadata,
  sha256Hex,
} from "../nova-knowledge/knowledge_core.mjs";
import {
  adaptDeviceCatalogRow,
  adaptGuardianIncidentRow,
  adaptGuardianLearningRow,
  adaptOperationalRows,
  adaptSupportTicketRow,
} from "../nova-knowledge/internal_adapters.mjs";

export const MAX_INGESTION_BATCH = 50;
export const SAFE_INGESTION_ADAPTERS = Object.freeze(["catalogue", "guardian", "support", "inventory", "sales", "valuation", "all"]);
const ADAPTERS = new Set(SAFE_INGESTION_ADAPTERS);
const F = "id,category,title,content,source_type,source_label,source_filename,mime_type,version_label,trust_level,status,content_hash,revision,metadata,created_by,updated_by,created_at,updated_at";
const clean = (value, max = 220) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value, fallback, max) => Math.min(Math.max(Number(value) || fallback, 1), max);
const authorityFor = (trust) => trust === "verified" ? 1 : trust === "reviewed" ? 0.82 : 0.65;
const validIso = (value) => {
  const raw = clean(value, 80);
  if (!raw) return null;
  const parsed = Date.parse(raw);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : null;
};

async function fetchRows(admin, table, columns, limit, offset, configure) {
  let query = admin.from(table).select(columns).range(offset, offset + limit - 1);
  if (configure) query = configure(query);
  const { data, error } = await query;
  if (error) throw error;
  return data || [];
}

async function fetchDocuments(admin, adapter, limit, offset) {
  const documents = [];

  if (adapter === "catalogue" || adapter === "all") {
    const rows = await fetchRows(admin, "device_catalog", "id,category,brand,family,model_name,model_number,release_year,release_date,ram_options,storage_options,key_specs,aliases,source_url,source_name,source_checked_at,active,market_region,sim_configuration,physical_sim_slots,esim_supported,dual_sim_supported,created_at,updated_at", limit, offset, (query) => query.eq("active", true).order("id", { ascending: true }));
    for (const row of rows) documents.push({ adapter: "device_catalog", sourceIdentity: String(row.id), document: adaptDeviceCatalogRow(row) });
  }

  if (adapter === "guardian" || adapter === "all") {
    const incidents = await fetchRows(admin, "guardian_incidents", "id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,auto_fix_eligible,requires_approval,attempt_count,github_branch,github_pr_number,last_error_code,applied_at,verified_at,created_at,updated_at,worker_version,reproduction_summary,test_plan,resolution_summary,occurrence_count,first_seen_at,last_seen_at,app_version,route,diagnostic_kind,diagnostic_message", limit, offset, (query) => query.order("updated_at", { ascending: false }));
    for (const row of incidents) documents.push({ adapter: "guardian_incidents", sourceIdentity: String(row.id), document: adaptGuardianIncidentRow(row) });

    const lessons = await fetchRows(admin, "nova_learning_experiences", "id,domain,lesson_key,lesson_type,summary,source_type,evidence,outcome,confidence,verified,active,observed_at,created_at,updated_at", limit, offset, (query) => query.eq("active", true).order("updated_at", { ascending: false }));
    for (const row of lessons) documents.push({ adapter: "nova_learning_experiences", sourceIdentity: String(row.id), document: adaptGuardianLearningRow(row) });
  }

  if (adapter === "support" || adapter === "all") {
    const tickets = await fetchRows(admin, "support_tickets", "id,category,status,priority,app_version,app_version_code,device_model,android_version,created_at,updated_at,resolved_at,closed_at", limit, offset, (query) => query.order("updated_at", { ascending: false }));
    for (const row of tickets) documents.push({ adapter: "support_tickets", sourceIdentity: String(row.id), document: adaptSupportTicketRow(row) });
  }

  if (adapter === "inventory" || adapter === "all") {
    const rows = await fetchRows(admin, "inventory_items", "status,acquired_price,expected_sale_price,acquired_at,listed_at,retired_at,created_at,updated_at", Math.min(limit * 10, 500), offset, (query) => query.order("updated_at", { ascending: false }));
    for (const document of adaptOperationalRows("inventory", rows)) documents.push({ adapter: "inventory_aggregate", sourceIdentity: `window:${offset}`, document });
  }

  if (adapter === "sales" || adapter === "all") {
    const rows = await fetchRows(admin, "sales_records", "acquired_cost,sold_price,fees,other_costs,realised_profit,sales_channel,sold_at,created_at", Math.min(limit * 10, 500), offset, (query) => query.order("sold_at", { ascending: false }));
    for (const document of adaptOperationalRows("sales", rows)) documents.push({ adapter: "sales_aggregate", sourceIdentity: `window:${offset}`, document });
  }

  if (adapter === "valuation" || adapter === "all") {
    const history = await fetchRows(admin, "valuation_history", "asking_price,market_value,max_buy,expected_profit,status,bought_price,sold_price,actual_profit,created_at,updated_at", Math.min(limit * 10, 500), offset, (query) => query.order("updated_at", { ascending: false }));
    const quotes = await fetchRows(admin, "valuation_quotes", "proposed_buy_cents,target_resale_cents,expected_margin_cents,confidence,recommendation,created_at", Math.min(limit * 10, 500), offset, (query) => query.order("created_at", { ascending: false }));
    for (const document of adaptOperationalRows("valuation", [...history, ...quotes])) documents.push({ adapter: "valuation_aggregate", sourceIdentity: `window:${offset}`, document });
  }

  return documents;
}

async function beginRun(admin, adapter, actorId, offset, limit) {
  const { data, error } = await admin.from("nova_knowledge_ingestion_runs").insert({
    adapter,
    domain: adapter === "all" ? "multi" : adapter,
    status: "running",
    checkpoint: { offset, limit },
    created_by: actorId || null,
    metadata: { bounded: true, source: "nova_internal_ingestion" },
  }).select("id").single();
  if (error) throw error;
  return data.id;
}

async function finishRun(admin, runId, status, stats, errorSummary = null) {
  const { error } = await admin.from("nova_knowledge_ingestion_runs").update({
    status,
    scanned_count: stats.scanned_count,
    created_count: stats.created_count,
    updated_count: stats.updated_count,
    skipped_count: stats.skipped_count,
    error_count: stats.error_count,
    error_summary: errorSummary,
    checkpoint: { ...stats.checkpoint, adopted_count: stats.adopted_count },
    completed_at: new Date().toISOString(),
  }).eq("id", runId);
  if (error) throw error;
}

async function snap(admin, row, actorId) {
  const { error } = await admin.from("nova_knowledge_revisions").upsert({
    knowledge_id: row.id,
    revision: row.revision,
    snapshot: {
      category: row.category, title: row.title, content: row.content, source_type: row.source_type,
      source_label: row.source_label, source_filename: row.source_filename, mime_type: row.mime_type,
      version_label: row.version_label, trust_level: row.trust_level, status: row.status,
      content_hash: row.content_hash, revision: row.revision, metadata: sanitizeMetadata(row.metadata || {}),
      created_at: row.created_at, updated_at: row.updated_at,
    },
    changed_by: actorId || null,
  }, { onConflict: "knowledge_id,revision", ignoreDuplicates: true });
  if (error) throw error;
}

async function hasLegacyCatalogueCoverage(admin, sourceIdentity) {
  const deviceCatalogId = Number(sourceIdentity);
  if (!Number.isSafeInteger(deviceCatalogId) || deviceCatalogId <= 0) return false;
  const { data, error } = await admin.from("nova_knowledge_items").select("id").eq("category", "catalogue").eq("status", "active").contains("metadata", { generated_from_live_catalogue: true, device_catalog_id: deviceCatalogId }).limit(1);
  if (error) throw error;
  return Boolean(data?.length);
}

async function upsertSourceAndChunks(admin, row, normalized, refreshOnly = false) {
  const metadata = normalized.metadata || {};
  const observedAt = validIso(metadata.observed_at) || row.updated_at || new Date().toISOString();
  const staleAfter = validIso(metadata.stale_after);
  const confidence = Math.min(Math.max(Number(metadata.confidence) || 0.7, 0), 1);
  const sourcePayload = {
    source_key: `internal:${metadata.adapter_key}`,
    domain: clean(metadata.domain, 80).toLowerCase() || normalized.category,
    source_type: "import",
    source_label: normalized.source_label,
    source_uri: clean(metadata.source_uri, 1000) || null,
    trust_level: normalized.trust_level,
    authority_score: authorityFor(normalized.trust_level),
    content_hash: normalized.content_hash,
    status: "active",
    observed_at: observedAt,
    retrieved_at: new Date().toISOString(),
    stale_after: staleAfter,
    metadata: sanitizeMetadata(metadata),
    updated_at: new Date().toISOString(),
  };
  const { data: source, error: sourceError } = await admin.from("nova_knowledge_sources").upsert(sourcePayload, { onConflict: "source_key" }).select("id").single();
  if (sourceError) throw sourceError;

  if (refreshOnly) {
    const { error } = await admin.from("nova_knowledge_chunks").update({ source_id: source.id, trust_level: normalized.trust_level, confidence, observed_at: observedAt, stale_after: staleAfter, metadata: { domain: sourcePayload.domain, adapter_key: metadata.adapter_key }, updated_at: new Date().toISOString() }).eq("knowledge_id", row.id).eq("knowledge_revision", Number(row.revision || 1)).eq("status", "active");
    if (error) throw error;
    return;
  }

  const chunks = await chunkKnowledgeDocument(normalized);
  const { error: supersedeError } = await admin.from("nova_knowledge_chunks").update({ status: "superseded", updated_at: new Date().toISOString() }).eq("knowledge_id", row.id).eq("status", "active");
  if (supersedeError) throw supersedeError;
  if (!chunks.length) return;

  const chunkRows = chunks.map((chunk) => ({
    knowledge_id: row.id, source_id: source.id, knowledge_revision: Number(row.revision || 1), chunk_index: chunk.chunk_index,
    content: chunk.content, content_hash: chunk.content_hash, char_start: chunk.char_start, char_end: chunk.char_end,
    token_estimate: chunk.token_estimate, trust_level: normalized.trust_level, confidence, observed_at: observedAt,
    stale_after: staleAfter, status: "active", embedding_status: "pending", embedding_error: null,
    metadata: { domain: sourcePayload.domain, adapter_key: metadata.adapter_key }, updated_at: new Date().toISOString(),
  }));
  const { error: chunkError } = await admin.from("nova_knowledge_chunks").upsert(chunkRows, { onConflict: "knowledge_id,knowledge_revision,chunk_index" });
  if (chunkError) throw chunkError;
}

async function persistDocument(admin, entry, actorId) {
  if (entry.adapter === "device_catalog" && await hasLegacyCatalogueCoverage(admin, entry.sourceIdentity)) return "adopted";
  const adapterKey = await sha256Hex(`${entry.adapter}:${entry.sourceIdentity}`);
  const normalized = await normalizeKnowledgeDocument({ ...entry.document, metadata: { ...(entry.document.metadata || {}), managed_by: "nova_internal_adapter", adapter: entry.adapter, adapter_key: adapterKey } });
  if (!normalized.title || !normalized.content) return "skipped";

  const { data: existing, error: findError } = await admin.from("nova_knowledge_items").select(F).eq("source_type", "import").contains("metadata", { managed_by: "nova_internal_adapter", adapter_key: adapterKey }).limit(1).maybeSingle();
  if (findError) throw findError;
  const now = new Date().toISOString();

  if (existing && existing.content_hash === normalized.content_hash && existing.status === "active") {
    const { data: refreshed, error } = await admin.from("nova_knowledge_items").update({ metadata: normalized.metadata, trust_level: normalized.trust_level, source_label: normalized.source_label, updated_by: actorId || null, updated_at: now }).eq("id", existing.id).select(F).single();
    if (error) throw error;
    await upsertSourceAndChunks(admin, refreshed, normalized, true);
    return "skipped";
  }

  if (existing) {
    await snap(admin, existing, actorId);
    const { data: updated, error } = await admin.from("nova_knowledge_items").update({ category: normalized.category, title: normalized.title, content: normalized.content, source_type: normalized.source_type, source_label: normalized.source_label, source_filename: normalized.source_filename, version_label: normalized.version_label, trust_level: normalized.trust_level, status: "active", content_hash: normalized.content_hash, revision: Number(existing.revision || 1) + 1, metadata: normalized.metadata, updated_by: actorId || null, updated_at: now }).eq("id", existing.id).select(F).single();
    if (error) throw error;
    await upsertSourceAndChunks(admin, updated, normalized, false);
    return "updated";
  }

  const { data: created, error } = await admin.from("nova_knowledge_items").insert({ category: normalized.category, title: normalized.title, content: normalized.content, source_type: normalized.source_type, source_label: normalized.source_label, source_filename: normalized.source_filename, version_label: normalized.version_label, trust_level: normalized.trust_level, status: "active", content_hash: normalized.content_hash, revision: 1, metadata: normalized.metadata, created_by: actorId || null, updated_by: actorId || null, created_at: now, updated_at: now }).select(F).single();
  if (error) throw error;
  await upsertSourceAndChunks(admin, created, normalized, false);
  return "created";
}

export async function latestIngestion(admin, adapter) {
  const { data, error } = await admin.from("nova_knowledge_ingestion_runs").select("completed_at,started_at,checkpoint,status").eq("adapter", adapter).in("status", ["completed", "partial"]).order("started_at", { ascending: false }).limit(1).maybeSingle();
  if (error) throw error;
  return data || null;
}

export async function runInternalIngestion({ admin, adapter = "all", limit = 20, offset = 0, actorId = null }) {
  const safeAdapter = clean(adapter, 40).toLowerCase() || "all";
  if (!ADAPTERS.has(safeAdapter)) throw new Error("Invalid ingestion adapter");
  const safeLimit = bounded(limit, 20, MAX_INGESTION_BATCH);
  const safeOffset = Math.max(Number(offset) || 0, 0);
  let runId = null;
  const stats = { scanned_count: 0, created_count: 0, updated_count: 0, skipped_count: 0, adopted_count: 0, error_count: 0, checkpoint: { offset: safeOffset, limit: safeLimit, next_offset: null } };
  try {
    runId = await beginRun(admin, safeAdapter, actorId, safeOffset, safeLimit);
    const documents = await fetchDocuments(admin, safeAdapter, safeLimit, safeOffset);
    stats.scanned_count = documents.length;
    for (const entry of documents) {
      try {
        const outcome = await persistDocument(admin, entry, actorId);
        if (outcome === "created") stats.created_count += 1;
        else if (outcome === "updated") stats.updated_count += 1;
        else if (outcome === "adopted") { stats.adopted_count += 1; stats.skipped_count += 1; }
        else stats.skipped_count += 1;
      } catch { stats.error_count += 1; }
    }
    if (documents.length) stats.checkpoint.next_offset = safeOffset + safeLimit;
    const status = stats.error_count > 0 ? "partial" : "completed";
    await finishRun(admin, runId, status, stats);
    return { ok: true, run_id: runId, adapter: safeAdapter, ...stats, documents: documents.length };
  } catch (error) {
    const message = clean(error instanceof Error ? error.message : error, 500);
    if (runId) {
      try { await finishRun(admin, runId, "failed", { ...stats, error_count: Math.max(1, stats.error_count) }, message); } catch { /* preserve original error */ }
    }
    throw error;
  }
}