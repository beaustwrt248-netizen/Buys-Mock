import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadCore() {
  const source = fs.readFileSync(new URL('../morley-ai-assessment-core.js', import.meta.url), 'utf8');
  const window = {};
  const context = vm.createContext({ window, globalThis: window, Object, Array, Number, String, Boolean, Math, Set, Map, JSON });
  vm.runInContext(source, context, { filename: 'morley-ai-assessment-core.js' });
  return window.MorleyAssessmentCore;
}

test('deal-risk evaluation raises review flags without automatically rejecting the device', () => {
  const core = loadCore();
  const result = core.evaluateDealRisk({
    duplicateIdentifier: true,
    identityMismatch: true,
    priceDeviationPct: 42,
    replacedComponentEvidence: true,
    unusualTransaction: false,
    identifierRef: 'sha256:abc123',
    rawIdentifier: '359999999999999',
  });
  assert.equal(result.decision, 'review_required');
  assert.equal(result.autoRejected, false);
  assert.equal(result.requiresStaffReview, true);
  assert.ok(result.flags.some((x) => x.type === 'duplicate_identifier'));
  assert.ok(result.flags.some((x) => x.type === 'identity_mismatch'));
  assert.ok(result.flags.some((x) => x.type === 'price_anomaly'));
  assert.ok(result.flags.some((x) => x.type === 'replaced_component'));
  assert.ok(result.flags.every((x) => x.status === 'open'));
  assert.doesNotMatch(JSON.stringify(result), /359999999999999/, 'raw IMEI/serial values must not be copied into AI risk output');
  assert.match(JSON.stringify(result), /sha256:abc123/, 'protected identifier references may be retained');
});

test('low-risk deal remains clear but still does not carry commercial authority', () => {
  const core = loadCore();
  const result = core.evaluateDealRisk({ priceDeviationPct: 5 });
  assert.equal(result.decision, 'clear');
  assert.equal(result.requiresStaffReview, false);
  assert.equal(result.commercialAuthority, 'advisory');
});

test('price anomaly threshold is deterministic and severity increases with deviation', () => {
  const core = loadCore();
  const medium = core.evaluateDealRisk({ priceDeviationPct: 26 });
  const high = core.evaluateDealRisk({ priceDeviationPct: 55 });
  assert.equal(medium.flags.find((x) => x.type === 'price_anomaly').severity, 'medium');
  assert.equal(high.flags.find((x) => x.type === 'price_anomaly').severity, 'high');
});

test('passport events are append-only records with explicit source and version', () => {
  const core = loadCore();
  const event = core.buildPassportEvent({
    eventType: 'diagnostic',
    assessmentId: 'assessment-1',
    details: { test: 'touch', status: 'pass' },
    source: 'staff',
    version: 'condition-v1',
  });
  assert.equal(event.eventType, 'diagnostic');
  assert.equal(event.assessmentId, 'assessment-1');
  assert.equal(event.source, 'staff');
  assert.equal(event.version, 'condition-v1');
  assert.deepEqual(event.details, { test: 'touch', status: 'pass' });
  assert.equal(Object.isFrozen(event), true);
  assert.equal(Object.isFrozen(event.details), true);
});

test('passport rejects unknown event types rather than creating ambiguous history', () => {
  const core = loadCore();
  assert.throws(() => core.buildPassportEvent({ eventType: 'mystery_event' }), /event type/i);
});
