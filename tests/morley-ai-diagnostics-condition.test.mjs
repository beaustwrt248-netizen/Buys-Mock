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

const STAFF_CHECKS = [
  ['Display', 'pass'],
  ['Touch', 'pass'],
  ['Camera', 'pass'],
  ['Speaker', 'fail'],
  ['Microphone', 'pass'],
  ['Charging', 'pass'],
  ['Buttons', 'unsupported'],
  ['Vibration', 'unavailable'],
  ['Connectivity', 'pass'],
].map(([name, state]) => ({ name, state, automated: false, platformVerified: false }));

test('maps the existing staff diagnostic states into the shared assessment vocabulary', () => {
  const core = loadCore();
  const diagnostics = core.normalizeStaffDiagnostics(STAFF_CHECKS);
  assert.equal(diagnostics.length, 9);
  assert.deepEqual(diagnostics.map((x) => x.test), ['display','touch','camera','speaker','microphone','charging','buttons','vibration','connectivity']);
  assert.deepEqual(diagnostics.map((x) => x.status), ['pass','pass','pass','fail','pass','pass','not_tested','not_tested','pass']);
  assert.equal(diagnostics.find((x) => x.test === 'buttons').staffVerified, false);
  assert.equal(diagnostics.find((x) => x.test === 'speaker').staffVerified, true);
});

test('never upgrades unsupported or unavailable checks to a pass', () => {
  const core = loadCore();
  const diagnostics = core.normalizeStaffDiagnostics([
    { name: 'Touch', state: 'unsupported', automated: true, platformVerified: true },
    { name: 'Charging', state: 'unavailable', automated: true, platformVerified: true },
  ]);
  assert.ok(diagnostics.every((x) => x.status === 'not_tested'));
});

test('only marks automation verified when the platform verification flag is also true', () => {
  const core = loadCore();
  const diagnostics = core.normalizeStaffDiagnostics([
    { name: 'Display', state: 'pass', automated: true, platformVerified: false },
    { name: 'Touch', state: 'pass', automated: true, platformVerified: true },
  ]);
  assert.equal(diagnostics[0].automatedVerified, false);
  assert.equal(diagnostics[1].automatedVerified, true);
});

test('builds the Morley Condition Score from staff diagnostics without hiding incomplete coverage', () => {
  const core = loadCore();
  const result = core.scoreStaffCondition({ cosmeticScore: 88, checks: STAFF_CHECKS });
  assert.equal(result.rulesVersion, core.CONDITION_RULES_VERSION);
  assert.equal(result.diagnosticsTotal, 9);
  assert.equal(result.diagnosticsObserved, 7);
  assert.ok(result.confidence > 0 && result.confidence < 1);
  assert.ok(result.overallScore >= 0 && result.overallScore <= 100);
  assert.ok(['excellent','good','fair','poor','faulty'].includes(result.recommendedGrade));
});

test('critical staff failures cap the condition recommendation at faulty', () => {
  const core = loadCore();
  const result = core.scoreStaffCondition({ cosmeticScore: 100, checks: [
    { name: 'Touch', state: 'fail' },
    { name: 'Speaker', state: 'pass' },
  ] });
  assert.equal(result.recommendedGrade, 'faulty');
  assert.equal(result.criticalFailure, true);
  assert.ok(result.overallScore <= 44);
});
