import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadClient({
  session = { user: { id: 'user-1' } },
  insertResult = { data: [{ id: 'assessment-1' }], error: null },
  tableResults = {},
  core = {},
} = {}) {
  const calls = [];
  const tables = new Map();
  const responseFor = (name) => tableResults[name] || insertResult;
  const sb = {
    auth: { getSession: async () => ({ data: { session }, error: null }) },
    from(name) {
      const api = {
        insert(payload) { calls.push({ op: 'insert', name, payload }); api._payload = payload; return api; },
        update(payload) { calls.push({ op: 'update', name, payload }); api._payload = payload; return api; },
        eq(column, value) { calls.push({ op: 'eq', name, column, value }); return api; },
        select() { calls.push({ op: 'select', name }); return api; },
        single: async () => ({ data: responseFor(name).data?.[0] || null, error: responseFor(name).error }),
        maybeSingle: async () => ({ data: null, error: null }),
        then(resolve) { return Promise.resolve(responseFor(name)).then(resolve); },
      };
      tables.set(name, api);
      return api;
    },
  };
  const source = fs.readFileSync(new URL('../morley-ai-assessment-client.js', import.meta.url), 'utf8');
  const window = {
    sb,
    MorleyAssessmentCore: {
      canPrepareStock: (v) => !!v?.ready,
      ...core,
    },
  };
  vm.runInNewContext(source, { window, globalThis: window, Object, Array, Number, String, Boolean, Math, JSON, Promise, Error, Date }, { filename: 'morley-ai-assessment-client.js' });
  return { client: window.MorleyAssessmentClient, calls };
}

test('requires an authenticated session before any assessment write', async () => {
  const { client, calls } = loadClient({ session: null });
  const result = await client.createAssessment({ resolvedModel: 'Pixel 10 Pro' });
  assert.equal(result.ok, false);
  assert.equal(result.code, 'auth_required');
  assert.equal(result.recoverable, true);
  assert.equal(calls.length, 0);
});

test('creates assessment with schema-safe fields and never persists raw identifier values', async () => {
  const { client, calls } = loadClient();
  const result = await client.createAssessment({
    catalogueRef: 'catalog-1',
    resolvedModel: 'Pixel 10 Pro',
    resolvedStorageGb: 256,
    identityConfidence: 0.91,
    rawIdentifier: '359999999999999',
    imei: '359999999999999',
  });
  assert.equal(result.ok, true);
  const write = calls.find((x) => x.name === 'device_assessments' && x.op === 'insert');
  assert.ok(write);
  const json = JSON.stringify(write.payload);
  assert.doesNotMatch(json, /359999999999999/);
  assert.equal(write.payload.catalogue_ref, 'catalog-1');
  assert.equal(write.payload.resolved_model, 'Pixel 10 Pro');
  assert.equal(write.payload.resolved_storage_gb, 256);
});

test('evidence accepts protected identifier references but strips raw identifiers from metadata', async () => {
  const { client, calls } = loadClient();
  const result = await client.addEvidence('assessment-1', {
    type: 'photo', source: 'device_lens', confidence: 0.8,
    protectedIdentifierRef: 'sha256:abc',
    metadata: { angle: 'front', imei: '359999999999999', serial: 'ABC123', note: 'verified front photo' },
  });
  assert.equal(result.ok, true);
  const write = calls.find((x) => x.name === 'assessment_evidence' && x.op === 'insert');
  const json = JSON.stringify(write.payload);
  assert.match(json, /sha256:abc/);
  assert.doesNotMatch(json, /359999999999999|ABC123/);
  assert.equal(write.payload.metadata.note, 'verified front photo');
});

test('diagnostic writes only the supported normalized state vocabulary', async () => {
  const { client, calls } = loadClient();
  const bad = await client.recordDiagnostic('assessment-1', { test: 'touch', status: 'unsupported' });
  assert.equal(bad.ok, false);
  assert.equal(bad.code, 'invalid_diagnostic_status');
  const good = await client.recordDiagnostic('assessment-1', { test: 'touch', status: 'pass', severity: 'critical', confidence: 1 });
  assert.equal(good.ok, true);
  const write = calls.find((x) => x.name === 'diagnostic_results' && x.op === 'insert');
  assert.equal(write.payload.test_type, 'touch');
  assert.equal(write.payload.status, 'pass');
});

test('persists Repair or Buy proposal, pending AI audit and passport event without granting commercial authority', async () => {
  const decision = Object.freeze({
    recommendation: 'parts_only',
    reason: 'Verified parts recovery produces the strongest margin while clearing the required minimum.',
    asIsMargin: 50,
    repairedMargin: 30,
    partsMargin: 140,
    repairCost: 90,
    partsProcessingCost: 20,
    rulesVersion: 'repair-v1',
    commercialAuthority: 'advisory',
    requiresStaffConfirmation: true,
    blockers: Object.freeze([]),
  });
  const { client, calls } = loadClient({
    tableResults: {
      valuation_quotes: { data: [{ id: 'quote-1' }], error: null },
      device_passports: { data: [{ id: 'passport-1' }], error: null },
      ai_decision_audit: { data: [{ id: 'audit-1' }], error: null },
      device_passport_events: { data: [{ id: 'event-1' }], error: null },
      device_assessments: { data: [{ id: 'assessment-1' }], error: null },
    },
    core: { decideRepairStrategy: () => decision },
  });

  const result = await client.persistRepairProposal('assessment-1', {
    commercialInputsVerified: true,
    buyCost: 100,
    resaleAsIs: 150,
    resaleAfterRepair: 220,
    repairCost: 90,
    partsRecoveryValue: 260,
    partsProcessingCost: 20,
    minMargin: 100,
    confidence: 0.92,
    modelVersion: 'rules-only',
    inputRefs: ['valuation:quote-0'],
  });

  assert.equal(result.ok, true);
  assert.equal(result.code, 'repair_proposal_recorded');
  assert.equal(result.data.recommendation, 'parts_only');
  assert.equal(result.data.requiresStaffConfirmation, true);

  const quote = calls.find((x) => x.name === 'valuation_quotes' && x.op === 'insert');
  assert.ok(quote);
  assert.equal(quote.payload.assessment_id, 'assessment-1');
  assert.equal(quote.payload.recommendation, 'parts_only');
  assert.equal(quote.payload.proposed_buy_cents, 10000);
  assert.equal(quote.payload.expected_margin_cents, 14000);
  assert.equal(quote.payload.confidence, 0.92);

  const assessment = calls.find((x) => x.name === 'device_assessments' && x.op === 'update');
  assert.ok(assessment);
  assert.equal(assessment.payload.state, 'proposed');
  assert.equal(assessment.payload.proposed_buy_price_cents, 10000);
  assert.equal(Object.hasOwn(assessment.payload, 'repair_decision'), false);

  const audit = calls.find((x) => x.name === 'ai_decision_audit' && x.op === 'insert');
  assert.ok(audit);
  assert.equal(audit.payload.decision_type, 'repair_or_buy');
  assert.equal(audit.payload.approval_status, 'pending');
  assert.equal(audit.payload.output.recommendation, 'parts_only');

  const passport = calls.find((x) => x.name === 'device_passports' && x.op === 'insert');
  assert.ok(passport);
  const event = calls.find((x) => x.name === 'device_passport_events' && x.op === 'insert');
  assert.ok(event);
  assert.equal(event.payload.passport_id, 'passport-1');
  assert.equal(event.payload.event_type, 'repair');
  assert.equal(event.payload.details.recommendation, 'parts_only');
  assert.equal(event.payload.details.commercialAuthority, 'advisory');
});

test('repair proposal remains review-required when the shared core blocks commercial inputs', async () => {
  const decision = Object.freeze({
    recommendation: 'review_required',
    reason: 'Commercial inputs are not verified; staff review is required.',
    asIsMargin: null,
    repairedMargin: null,
    partsMargin: null,
    repairCost: 50,
    rulesVersion: 'repair-v1',
    commercialAuthority: 'advisory',
    requiresStaffConfirmation: true,
    blockers: Object.freeze(['commercial_inputs_unverified']),
  });
  const { client, calls } = loadClient({ core: { decideRepairStrategy: () => decision } });
  const result = await client.persistRepairProposal('assessment-1', {
    commercialInputsVerified: false,
    buyCost: 100,
    resaleAsIs: 250,
    resaleAfterRepair: 300,
    repairCost: 50,
    minMargin: 100,
  });
  assert.equal(result.ok, true);
  assert.equal(result.data.recommendation, 'review_required');
  const assessment = calls.find((x) => x.name === 'device_assessments' && x.op === 'update');
  assert.equal(assessment.payload.state, 'review_required');
});

test('stock payload remains unavailable until the shared core says preparation is safe', async () => {
  const { client } = loadClient();
  const blocked = client.prepareStockPayload({ ready: false, model: 'Pixel 10 Pro' });
  assert.equal(blocked.ok, false);
  assert.equal(blocked.code, 'stock_preparation_blocked');
  const ready = client.prepareStockPayload({ ready: true, model: 'Pixel 10 Pro', storage: '256 GB', buyPrice: 420 });
  assert.equal(ready.ok, true);
  assert.equal(ready.payload.model, 'Pixel 10 Pro');
  assert.equal(ready.requiresStaffPublish, true);
});

test('client source never writes assessment identifiers or secrets to localStorage or console', () => {
  const source = fs.readFileSync(new URL('../morley-ai-assessment-client.js', import.meta.url), 'utf8');
  assert.doesNotMatch(source, /localStorage\.(?:setItem|removeItem)/);
  assert.doesNotMatch(source, /console\.(?:log|info|debug|warn|error)/);
  assert.doesNotMatch(source, /service[_-]?role/i);
});