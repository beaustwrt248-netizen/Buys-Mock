import { sanitizeMetadata } from './knowledge_core.mjs';

const text = (value, max = 240) => String(value ?? '').trim().replace(/\s+/g, ' ').slice(0, max);
const yesNo = (value) => value === true ? 'yes' : value === false ? 'no' : 'unknown';
const list = (value) => Array.isArray(value) ? value.map((item) => text(item, 80)).filter(Boolean) : [];
const number = (value) => Number.isFinite(Number(value)) ? Number(value) : null;
const iso = (value) => {
  const parsed = Date.parse(String(value ?? ''));
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : null;
};
const safeJson = (value) => {
  const sanitized = sanitizeMetadata(value && typeof value === 'object' ? value : {}) || {};
  const serialized = JSON.stringify(sanitized);
  return serialized === '{}' ? null : serialized.slice(0, 4000);
};
const compactLines = (lines) => lines.filter((line) => line && !line.endsWith(': ')).join('\n');

export function adaptDeviceCatalogRow(row = {}) {
  const brand = text(row.brand, 100);
  const model = text(row.model_name, 160);
  const modelNumber = text(row.model_number, 120);
  const family = text(row.family, 120);
  const ram = list(row.ram_options);
  const storage = list(row.storage_options);
  const aliases = list(row.aliases);
  const specs = safeJson(row.key_specs);
  const sourceUri = /^https?:\/\//i.test(String(row.source_url ?? '')) ? text(row.source_url, 1000) : null;
  const observedAt = iso(row.source_checked_at) || iso(row.updated_at) || iso(row.created_at);

  return {
    category: 'catalogue',
    title: text(`${brand} ${model}${modelNumber ? ` (${modelNumber})` : ''}`, 180),
    source_type: 'import',
    source_label: text(row.source_name, 220) || 'Device catalogue',
    trust_level: sourceUri ? 'reviewed' : 'reference',
    content: compactLines([
      `Device: ${text(`${brand} ${model}`, 220)}`,
      modelNumber ? `Model number: ${modelNumber}` : null,
      family ? `Family: ${family}` : null,
      row.category ? `Category: ${text(row.category, 80)}` : null,
      row.market_region ? `Market: ${text(row.market_region, 40)}` : null,
      row.release_date ? `Release date: ${text(row.release_date, 40)}` : row.release_year ? `Release year: ${text(row.release_year, 8)}` : null,
      ram.length ? `RAM: ${ram.join(', ')}` : null,
      storage.length ? `Storage: ${storage.join(', ')}` : null,
      row.sim_configuration ? `SIM configuration: ${text(row.sim_configuration, 120)}` : null,
      row.physical_sim_slots != null ? `Physical SIM slots: ${text(row.physical_sim_slots, 4)}` : null,
      row.esim_supported != null ? `eSIM supported: ${yesNo(row.esim_supported)}` : null,
      row.dual_sim_supported != null ? `Dual SIM supported: ${yesNo(row.dual_sim_supported)}` : null,
      aliases.length ? `Aliases: ${aliases.join(', ')}` : null,
      specs ? `Key specs: ${specs}` : null,
      row.active != null ? `Catalogue active: ${yesNo(row.active)}` : null,
    ]),
    metadata: {
      adapter: 'device_catalog',
      domain: 'catalogue',
      market_region: text(row.market_region, 40) || null,
      source_uri: sourceUri,
      observed_at: observedAt,
      stale_after: observedAt ? new Date(Date.parse(observedAt) + 180 * 86400000).toISOString() : null,
      confidence: sourceUri ? 0.9 : 0.72,
    },
  };
}

export function adaptSupportTicketRow(row = {}) {
  const resolved = Boolean(row.resolved_at || row.closed_at || ['resolved', 'closed'].includes(String(row.status ?? '').toLowerCase()));
  return {
    category: 'support',
    title: text(`Support pattern: ${text(row.category, 80) || 'uncategorised'} / ${text(row.status, 40) || 'unknown'}`, 180),
    source_type: 'import',
    source_label: 'Support ticket pattern',
    trust_level: resolved ? 'reviewed' : 'reference',
    content: compactLines([
      row.category ? `Category: ${text(row.category, 80)}` : null,
      row.status ? `Status: ${text(row.status, 40)}` : null,
      row.priority ? `Priority: ${text(row.priority, 40)}` : null,
      row.app_version ? `App version: ${text(row.app_version, 80)}` : null,
      row.app_version_code != null ? `App version code: ${text(row.app_version_code, 20)}` : null,
      row.device_model ? `Device model: ${text(row.device_model, 120)}` : null,
      row.android_version ? `Android version: ${text(row.android_version, 80)}` : null,
      `Resolved: ${yesNo(resolved)}`,
    ]),
    metadata: {
      adapter: 'support_tickets',
      domain: 'support',
      observed_at: iso(row.resolved_at) || iso(row.closed_at) || iso(row.updated_at) || iso(row.created_at),
      confidence: resolved ? 0.82 : 0.62,
      privacy_mode: 'structured_pattern_only',
    },
  };
}

export function adaptGuardianIncidentRow(row = {}) {
  const verified = String(row.state ?? '').toLowerCase() === 'verified' || Boolean(row.verified_at);
  const confidence = Math.min(1, Math.max(0, number(row.confidence) ?? 0.7));
  return {
    category: 'guardian',
    title: text(`Guardian ${text(row.classification, 100) || text(row.diagnostic_kind, 100) || 'incident'}: ${text(row.last_error_code, 80) || text(row.route, 80) || 'runtime evidence'}`, 180),
    source_type: 'import',
    source_label: 'Guardian incident',
    trust_level: verified ? 'verified' : 'reviewed',
    content: compactLines([
      row.source ? `Source: ${text(row.source, 100)}` : null,
      row.state ? `State: ${text(row.state, 60)}` : null,
      row.risk_level ? `Risk: ${text(row.risk_level, 40)}` : null,
      row.classification ? `Classification: ${text(row.classification, 100)}` : null,
      row.diagnosis_summary ? `Diagnosis: ${text(row.diagnosis_summary, 1200)}` : null,
      row.reproduction_summary ? `Reproduction: ${text(row.reproduction_summary, 1200)}` : null,
      row.test_plan ? `Test plan: ${text(row.test_plan, 1200)}` : null,
      row.resolution_summary ? `Resolution: ${text(row.resolution_summary, 1200)}` : null,
      row.proposed_action ? `Proposed action: ${text(row.proposed_action, 1000)}` : null,
      `Auto-fix eligible: ${yesNo(row.auto_fix_eligible)}`,
      `Requires approval: ${yesNo(row.requires_approval)}`,
      row.last_error_code ? `Error code: ${text(row.last_error_code, 120)}` : null,
      row.diagnostic_message ? `Diagnostic: ${text(row.diagnostic_message, 1200)}` : null,
      row.app_version ? `App version: ${text(row.app_version, 80)}` : null,
      row.route ? `Route: ${text(row.route, 200)}` : null,
      row.github_branch ? `Repair branch: ${text(row.github_branch, 180)}` : null,
      row.github_pr_number != null ? `Repair PR: #${text(row.github_pr_number, 12)}` : null,
      row.occurrence_count != null ? `Occurrences: ${text(row.occurrence_count, 12)}` : null,
    ]),
    metadata: {
      adapter: 'guardian_incidents',
      domain: 'guardian',
      observed_at: iso(row.verified_at) || iso(row.last_seen_at) || iso(row.updated_at) || iso(row.created_at),
      confidence,
      risk_level: text(row.risk_level, 40) || null,
      requires_approval: row.requires_approval === true,
      protected_boundary: true,
    },
  };
}

export function adaptGuardianLearningRow(row = {}) {
  const confidence = Math.min(1, Math.max(0, number(row.confidence) ?? 0.7));
  const safeEvidence = safeJson(row.evidence);
  return {
    category: 'guardian',
    title: text(`Guardian lesson: ${text(row.lesson_key, 140) || text(row.lesson_type, 100) || 'experience'}`, 180),
    source_type: 'import',
    source_label: 'Nova Guardian learning',
    trust_level: row.verified === true ? 'verified' : confidence >= 0.8 ? 'reviewed' : 'reference',
    content: compactLines([
      row.lesson_type ? `Lesson type: ${text(row.lesson_type, 100)}` : null,
      row.summary ? `Lesson: ${text(row.summary, 1800)}` : null,
      row.outcome ? `Outcome: ${text(row.outcome, 800)}` : null,
      safeEvidence ? `Evidence: ${safeEvidence}` : null,
      `Verified: ${yesNo(row.verified)}`,
    ]),
    metadata: {
      adapter: 'nova_learning_experiences',
      domain: text(row.domain, 80) || 'guardian',
      observed_at: iso(row.observed_at) || iso(row.updated_at) || iso(row.created_at),
      confidence,
      protected_boundary: true,
    },
  };
}

function aggregateNumbers(rows, keys) {
  const output = {};
  for (const key of keys) {
    const values = rows.map((row) => number(row[key])).filter((value) => value != null);
    if (!values.length) continue;
    output[key] = {
      count: values.length,
      total: Number(values.reduce((sum, value) => sum + value, 0).toFixed(2)),
      average: Number((values.reduce((sum, value) => sum + value, 0) / values.length).toFixed(2)),
    };
  }
  return output;
}

export function adaptOperationalRows(domain, rows = []) {
  if (!Array.isArray(rows) || rows.length === 0) return [];
  const safeDomain = ['inventory', 'sales', 'valuation'].includes(domain) ? domain : 'admin';
  const numericKeys = safeDomain === 'inventory'
    ? ['acquired_price', 'expected_sale_price']
    : safeDomain === 'sales'
      ? ['acquired_cost', 'sold_price', 'fees', 'other_costs', 'realised_profit']
      : ['asking_price', 'market_value', 'max_buy', 'expected_profit', 'bought_price', 'sold_price', 'actual_profit', 'proposed_buy_cents', 'target_resale_cents', 'expected_margin_cents'];
  const statuses = {};
  for (const row of rows) {
    const status = text(row.status || row.recommendation || row.sales_channel, 80) || 'unspecified';
    statuses[status] = (statuses[status] || 0) + 1;
  }
  const metrics = aggregateNumbers(rows, numericKeys);
  const observedAt = rows.map((row) => iso(row.updated_at) || iso(row.sold_at) || iso(row.created_at)).filter(Boolean).sort().at(-1) || null;
  return [{
    category: safeDomain === 'valuation' ? 'valuation' : safeDomain,
    title: `Operational ${safeDomain} snapshot`,
    source_type: 'import',
    source_label: `Authoritative ${safeDomain} aggregate`,
    trust_level: 'reviewed',
    content: compactLines([
      `Domain: ${safeDomain}`,
      `Records: ${rows.length}`,
      `Status/channel distribution: ${JSON.stringify(statuses)}`,
      Object.keys(metrics).length ? `Numeric aggregates: ${JSON.stringify(metrics)}` : null,
    ]),
    metadata: {
      adapter: `${safeDomain}_aggregate`,
      domain: safeDomain,
      observed_at: observedAt,
      confidence: 0.9,
      aggregate_only: true,
      record_count: rows.length,
    },
  }];
}
