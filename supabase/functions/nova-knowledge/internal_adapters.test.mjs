import test from 'node:test';
import assert from 'node:assert/strict';

import {
  adaptDeviceCatalogRow,
  adaptGuardianIncidentRow,
  adaptGuardianLearningRow,
  adaptSupportTicketRow,
  adaptOperationalRows,
} from './internal_adapters.mjs';

test('device adapter keeps Australian catalogue evidence and excludes opaque identifiers', () => {
  const doc = adaptDeviceCatalogRow({
    id: 987,
    category: 'mobile_phone', brand: 'Moto', family: 'Edge', model_name: 'Edge 70', model_number: 'XT-TEST',
    release_year: 2026, ram_options: ['8GB'], storage_options: ['256GB'], market_region: 'AU',
    sim_configuration: 'Dual SIM', physical_sim_slots: 2, esim_supported: true, dual_sim_supported: true,
    source_name: 'Motorola Australia', source_url: 'https://example.test/device', source_checked_at: '2026-09-12T00:00:00Z', active: true,
    key_specs: { display: '6.7 inch' }, aliases: ['Edge Seventy'], search_text: 'should not be copied', image_reference_url: 'https://example.test/image.jpg',
  });
  assert.equal(doc.category, 'catalogue');
  assert.match(doc.content, /Moto Edge 70/);
  assert.match(doc.content, /Market: AU/);
  assert.match(doc.content, /Storage: 256GB/);
  assert.doesNotMatch(doc.content, /987|should not be copied/);
  assert.equal(doc.metadata.source_uri, 'https://example.test/device');
});

test('support adapter learns safe issue patterns without copying customer text or identifiers', () => {
  const doc = adaptSupportTicketRow({
    id: 'ticket-secret-id', user_id: 'user-secret-id', assigned_to: 'agent-secret-id',
    category: 'login', subject: 'My email is person@example.com', description: 'Password reset token ABC123',
    status: 'resolved', priority: 'high', app_version: '2.15.101', app_version_code: 215101,
    device_model: 'SM-S948B', android_version: '16', diagnostics: { access_token: 'secret', crash: 'raw dump' }, diagnostics_opt_in: true,
    created_at: '2026-09-12T01:00:00Z', updated_at: '2026-09-12T02:00:00Z', resolved_at: '2026-09-12T02:00:00Z',
  });
  const serialized = JSON.stringify(doc);
  assert.equal(doc.category, 'support');
  assert.match(doc.content, /Category: login/);
  assert.match(doc.content, /Status: resolved/);
  assert.doesNotMatch(serialized, /person@example\.com|ABC123|ticket-secret-id|user-secret-id|agent-secret-id|access_token|raw dump/);
});

test('Guardian incident adapter preserves approval boundary evidence and strips dispatch/actor identifiers', () => {
  const doc = adaptGuardianIncidentRow({
    id: 'incident-id', ticket_id: 'ticket-id', approved_by: 'actor-id', dispatch_token: 'dispatch-secret',
    source: 'runtime', state: 'verified', risk_level: 'high', classification: 'runtime_error', confidence: 0.96,
    diagnosis_summary: 'recent is not defined', proposed_action: 'prepare guarded repair', auto_fix_eligible: false, requires_approval: true,
    github_branch: 'guardian/fix-recent', github_pr_number: 123, last_error_code: 'ReferenceError', worker_version: 'v2',
    reproduction_summary: 'Open dashboard', test_plan: 'Run runtime regression', resolution_summary: 'Source repaired',
    occurrence_count: 4, first_seen_at: '2026-09-10T00:00:00Z', last_seen_at: '2026-09-12T00:00:00Z', app_version: '2.15.101', route: '/dashboard', diagnostic_kind: 'runtime', diagnostic_message: 'recent is not defined', diagnostic_metadata: { token: 'bad' },
    updated_at: '2026-09-12T00:00:00Z', verified_at: '2026-09-12T00:00:00Z',
  });
  const serialized = JSON.stringify(doc);
  assert.equal(doc.category, 'guardian');
  assert.match(doc.content, /Requires approval: yes/);
  assert.match(doc.content, /Auto-fix eligible: no/);
  assert.doesNotMatch(serialized, /incident-id|ticket-id|actor-id|dispatch-secret|diagnostic_metadata/);
});

test('verified Guardian learning becomes reviewed or verified evidence without creator IDs', () => {
  const doc = adaptGuardianLearningRow({
    id: 'learning-id', domain: 'guardian', lesson_key: 'runtime.recent', lesson_type: 'repair_outcome', summary: 'Map srcdoc error to generator source',
    source_type: 'guardian', source_id: 'incident-id', evidence: { safe: 'regression passed', token: 'remove' }, outcome: 'verified', confidence: 0.94,
    verified: true, active: true, observed_at: '2026-09-12T00:00:00Z', created_by: 'actor-id', updated_at: '2026-09-12T00:00:00Z',
  });
  const serialized = JSON.stringify(doc);
  assert.equal(doc.trust_level, 'verified');
  assert.match(doc.content, /Map srcdoc error to generator source/);
  assert.doesNotMatch(serialized, /learning-id|incident-id|actor-id|remove/);
});

test('operational adapter emits nothing when authoritative rows are absent', () => {
  assert.deepEqual(adaptOperationalRows('inventory', []), []);
  assert.deepEqual(adaptOperationalRows('sales', []), []);
  assert.deepEqual(adaptOperationalRows('valuation', []), []);
});
