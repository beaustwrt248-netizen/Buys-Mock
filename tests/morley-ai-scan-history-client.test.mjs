import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadClient({ rows = [{ id: 'assessment-1', source: 'device_lens', checkpoint: 'analysis_failed' }] } = {}) {
  const calls = [];
  const sb = {
    auth: { getSession: async () => ({ data: { session: { user: { id: 'user-1' } } }, error: null }) },
    from(name) {
      const api = {
        insert(payload) { calls.push({ op: 'insert', name, payload }); return api; },
        update(payload) { calls.push({ op: 'update', name, payload }); return api; },
        select(columns) { calls.push({ op: 'select', name, columns }); return api; },
        eq(column, value) { calls.push({ op: 'eq', name, column, value }); return api; },
        order(column, options) { calls.push({ op: 'order', name, column, options }); return api; },
        limit(value) { calls.push({ op: 'limit', name, value }); return api; },
        single: async () => ({ data: rows[0] || null, error: null }),
        then(resolve) { return Promise.resolve({ data: rows, error: null }).then(resolve); },
      };
      return api;
    },
  };
  const source = fs.readFileSync(new URL('../morley-ai-assessment-client.js', import.meta.url), 'utf8');
  const window = { sb, MorleyAssessmentCore: { canPrepareStock: () => true } };
  vm.runInNewContext(source, { window, globalThis: window, Object, Array, Number, String, Boolean, Math, JSON, Promise, Error, Date, Set }, { filename: 'morley-ai-assessment-client.js' });
  return { client: window.MorleyAssessmentClient, calls };
}

test('Device Lens assessment can be created at capture start with source and checkpoint', async () => {
  const { client, calls } = loadClient();
  await client.createAssessment({ state: 'draft', source: 'device_lens', checkpoint: 'capture_started' });
  const write = calls.find((call) => call.op === 'insert' && call.name === 'device_assessments');
  assert.equal(write.payload.source, 'device_lens');
  assert.equal(write.payload.checkpoint, 'capture_started');
  assert.ok(write.payload.last_checkpoint_at);
});

test('checkpointAssessment persists failed and cancelled scans instead of deleting them', async () => {
  const { client, calls } = loadClient();
  const failed = await client.checkpointAssessment('assessment-1', {
    checkpoint: 'analysis_failed',
    errorCode: 'provider_timeout',
    metadata: { side: 'both', imei: '359999999999999' },
  });
  assert.equal(failed.ok, true);
  const write = calls.find((call) => call.op === 'update' && call.name === 'device_assessments');
  assert.equal(write.payload.checkpoint, 'analysis_failed');
  assert.equal(write.payload.last_error_code, 'provider_timeout');
  assert.doesNotMatch(JSON.stringify(write.payload), /359999999999999/);
  assert.equal(calls.some((call) => call.op === 'delete'), false);
});

test('checkpointAssessment rejects unsupported checkpoint names', async () => {
  const { client } = loadClient();
  const result = await client.checkpointAssessment('assessment-1', { checkpoint: 'invented_state' });
  assert.equal(result.ok, false);
  assert.equal(result.code, 'invalid_checkpoint');
});

test('listScanHistory reads Device Lens sessions newest first', async () => {
  const { client, calls } = loadClient();
  const result = await client.listScanHistory({ limit: 25 });
  assert.equal(result.ok, true);
  assert.equal(result.data.length, 1);
  assert.ok(calls.some((call) => call.op === 'eq' && call.column === 'source' && call.value === 'device_lens'));
  assert.ok(calls.some((call) => call.op === 'order' && call.column === 'created_at'));
  assert.ok(calls.some((call) => call.op === 'limit' && call.value === 25));
});
