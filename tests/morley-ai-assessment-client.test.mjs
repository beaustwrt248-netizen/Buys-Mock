import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadClient({ session = { user: { id: 'user-1' } }, insertResult = { data: [{ id: 'assessment-1' }], error: null } } = {}) {
  const calls = [];
  const tables = new Map();
  const sb = {
    auth: { getSession: async () => ({ data: { session }, error: null }) },
    from(name) {
      const api = {
        insert(payload) { calls.push({ op: 'insert', name, payload }); api._payload = payload; return api; },
        update(payload) { calls.push({ op: 'update', name, payload }); api._payload = payload; return api; },
        eq(column, value) { calls.push({ op: 'eq', name, column, value }); return api; },
        select() { calls.push({ op: 'select', name }); return api; },
        single: async () => ({ data: insertResult.data?.[0] || null, error: insertResult.error }),
        then(resolve) { return Promise.resolve(insertResult).then(resolve); },
      };
      tables.set(name, api);
      return api;
    },
  };
  const source = fs.readFileSync(new URL('../morley-ai-assessment-client.js', import.meta.url), 'utf8');
  const window = { sb, MorleyAssessmentCore: { canPrepareStock: (v) => !!v?.ready } };
  vm.runInNewContext(source, { window, globalThis: window, Object, Array, Number, String, Boolean, Math, JSON, Promise, Error }, { filename: 'morley-ai-assessment-client.js' });
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
