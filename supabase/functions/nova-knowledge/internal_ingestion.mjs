import {
  chunkKnowledgeDocument,
  normalizeKnowledgeDocument,
  sanitizeMetadata,
  sha256Hex,
} from './knowledge_core.mjs';

const F = 'id,category,title,content,source_type,source_label,source_filename,mime_type,version_label,trust_level,status,content_hash,revision,metadata,created_by,updated_by,created_at,updated_at';

const clean = (value, max = 220) => String(value ?? '').trim().replace(/\s+/g, ' ').slice(0, max);
const authorityFor = (trust) => trust === 'verified' ? 1 : trust === 'reviewed' ? 0.82 : 0.65;
const validIso = (value) => {
  const raw = clean(value, 80);
  if (!raw) return null;
  const parsed = Date.parse(raw);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : null;
};

export function createInternalIngestionEngine({ admin }) {
  if (!admin) throw new Error('Nova internal ingestion requires an admin client');

  async function hasLegacyCatalogueCoverage(sourceIdentity) {
    const deviceCatalogId = Number(sourceIdentity);
    if (!Number.isSafeInteger(deviceCatalogId) || deviceCatalogId <= 0) return false;
    const { data, error } = await admin.from('nova_knowledge_items')
      .select('id')
      .eq('category', 'catalogue')
      .eq('status', 'active')
      .contains('metadata', { generated_from_live_catalogue: true, device_catalog_id: deviceCatalogId })
      .limit(1);
    if (error) throw error;
    return Boolean(data?.length);
  }

  async function snap(row, actorId) {
    const { error } = await admin.from('nova_knowledge_revisions').upsert({
      knowledge_id: row.id,
      revision: row.revision,
      snapshot: {
        category: row.category,
        title: row.title,
        content: row.content,
        source_type: row.source_type,
        source_label: row.source_label,
        source_filename: row.source_filename,
        mime_type: row.mime_type,
        version_label: row.version_label,
        trust_level: row.trust_level,
        status: row.status,
        content_hash: row.content_hash,
        revision: row.revision,
        metadata: sanitizeMetadata(row.metadata || {}),
        created_at: row.created_at,
        updated_at: row.updated_at,
      },
      changed_by: actorId ?? null,
    }, { onConflict: 'knowledge_id,revision', ignoreDuplicates: true });
    if (error) throw error;
  }

  async function upsertSourceAndChunks(row, normalized, adapterKey, refreshOnly = false) {
    const metadata = normalized.metadata || {};
    const observedAt = validIso(metadata.observed_at) || row.updated_at || new Date().toISOString();
    const staleAfter = validIso(metadata.stale_after);
    const confidence = Math.min(Math.max(Number(metadata.confidence) || 0.7, 0), 1);
    const sourcePayload = {
      source_key: `internal:${adapterKey}`,
      domain: clean(metadata.domain, 80).toLowerCase() || normalized.category,
      source_type: 'import',
      source_label: normalized.source_label,
      source_uri: clean(metadata.source_uri, 1000) || null,
      trust_level: normalized.trust_level,
      authority_score: authorityFor(normalized.trust_level),
      content_hash: normalized.content_hash,
      status: 'active',
      observed_at: observedAt,
      retrieved_at: new Date().toISOString(),
      stale_after: staleAfter,
      metadata: sanitizeMetadata(metadata),
      updated_at: new Date().toISOString(),
    };
    const { data: source, error: sourceError } = await admin.from('nova_knowledge_sources')
      .upsert(sourcePayload, { onConflict: 'source_key' })
      .select('id')
      .single();
    if (sourceError) throw sourceError;

    if (refreshOnly) {
      const { error } = await admin.from('nova_knowledge_chunks').update({
        source_id: source.id,
        trust_level: normalized.trust_level,
        confidence,
        observed_at: observedAt,
        stale_after: staleAfter,
        metadata: { domain: sourcePayload.domain, adapter_key: adapterKey },
        updated_at: new Date().toISOString(),
      }).eq('knowledge_id', row.id).eq('knowledge_revision', Number(row.revision || 1)).eq('status', 'active');
      if (error) throw error;
      return;
    }

    const chunks = await chunkKnowledgeDocument(normalized);
    const { error: supersedeError } = await admin.from('nova_knowledge_chunks')
      .update({ status: 'superseded', updated_at: new Date().toISOString() })
      .eq('knowledge_id', row.id)
      .eq('status', 'active');
    if (supersedeError) throw supersedeError;
    if (!chunks.length) return;

    const rows = chunks.map((chunk) => ({
      knowledge_id: row.id,
      source_id: source.id,
      knowledge_revision: Number(row.revision || 1),
      chunk_index: chunk.chunk_index,
      content: chunk.content,
      content_hash: chunk.content_hash,
      char_start: chunk.char_start,
      char_end: chunk.char_end,
      token_estimate: chunk.token_estimate,
      trust_level: normalized.trust_level,
      confidence,
      observed_at: observedAt,
      stale_after: staleAfter,
      status: 'active',
      embedding_status: 'pending',
      embedding_error: null,
      metadata: { domain: sourcePayload.domain, adapter_key: adapterKey },
      updated_at: new Date().toISOString(),
    }));
    const { error } = await admin.from('nova_knowledge_chunks')
      .upsert(rows, { onConflict: 'knowledge_id,knowledge_revision,chunk_index' });
    if (error) throw error;
  }

  async function persistDocument(entry, { actorId = null } = {}) {
    if (entry.adapter === 'device_catalog' && await hasLegacyCatalogueCoverage(entry.sourceIdentity)) {
      return 'adopted';
    }

    const adapterKey = await sha256Hex(`${entry.adapter}:${entry.sourceIdentity}`);
    const normalized = await normalizeKnowledgeDocument({
      ...entry.document,
      metadata: {
        ...(entry.document.metadata || {}),
        managed_by: 'nova_internal_adapter',
        adapter: entry.adapter,
        adapter_key: adapterKey,
      },
    });
    if (!normalized.title || !normalized.content) return 'skipped';

    const { data: existing, error: findError } = await admin.from('nova_knowledge_items')
      .select(F)
      .eq('source_type', 'import')
      .contains('metadata', { managed_by: 'nova_internal_adapter', adapter_key: adapterKey })
      .limit(1)
      .maybeSingle();
    if (findError) throw findError;
    const now = new Date().toISOString();

    if (existing && existing.content_hash === normalized.content_hash && existing.status === 'active') {
      const { data: refreshed, error: refreshError } = await admin.from('nova_knowledge_items').update({
        metadata: normalized.metadata,
        trust_level: normalized.trust_level,
        source_label: normalized.source_label,
        updated_by: actorId ?? null,
        updated_at: now,
      }).eq('id', existing.id).select(F).single();
      if (refreshError) throw refreshError;
      await upsertSourceAndChunks(refreshed, normalized, adapterKey, true);
      return 'skipped';
    }

    if (existing) {
      await snap(existing, actorId);
      const { data: updated, error } = await admin.from('nova_knowledge_items').update({
        category: normalized.category,
        title: normalized.title,
        content: normalized.content,
        source_type: normalized.source_type,
        source_label: normalized.source_label,
        source_filename: normalized.source_filename,
        version_label: normalized.version_label,
        trust_level: normalized.trust_level,
        status: 'active',
        content_hash: normalized.content_hash,
        revision: Number(existing.revision || 1) + 1,
        metadata: normalized.metadata,
        updated_by: actorId ?? null,
        updated_at: now,
      }).eq('id', existing.id).select(F).single();
      if (error) throw error;
      await upsertSourceAndChunks(updated, normalized, adapterKey, false);
      return 'updated';
    }

    const { data: created, error } = await admin.from('nova_knowledge_items').insert({
      category: normalized.category,
      title: normalized.title,
      content: normalized.content,
      source_type: normalized.source_type,
      source_label: normalized.source_label,
      source_filename: normalized.source_filename,
      version_label: normalized.version_label,
      trust_level: normalized.trust_level,
      status: 'active',
      content_hash: normalized.content_hash,
      revision: 1,
      metadata: normalized.metadata,
      created_by: actorId ?? null,
      updated_by: actorId ?? null,
      created_at: now,
      updated_at: now,
    }).select(F).single();
    if (error) throw error;
    await upsertSourceAndChunks(created, normalized, adapterKey, false);
    return 'created';
  }

  return { persistDocument };
}
