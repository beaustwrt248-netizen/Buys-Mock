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

test('normalizes evidence confidence into the 0..1 range', () => {
  const core = loadCore();
  const items = core.normalizeEvidence([
    { type: 'front_photo', confidence: 1.7, verified: true },
    { type: 'rear_photo', confidence: -0.4 },
    { type: 'label', confidence: '0.62' },
  ]);
  assert.equal(items[0].confidence, 1);
  assert.equal(items[1].confidence, 0);
  assert.equal(items[2].confidence, 0.62);
  assert.equal(items[0].verified, true);
});

test('blocks state progression when identity is unresolved', () => {
  const core = loadCore();
  assert.equal(core.resolveAssessmentState({
    identityResolved: false,
    storageResolved: true,
    evidenceSufficient: true,
  }), 'identity_unresolved');
});

test('blocks state progression when storage is unresolved', () => {
  const core = loadCore();
  assert.equal(core.resolveAssessmentState({
    identityResolved: true,
    storageResolved: false,
    evidenceSufficient: true,
  }), 'storage_unresolved');
});

test('scores cosmetic and tested functional evidence deterministically', () => {
  const core = loadCore();
  const result = core.scoreCondition({
    cosmeticScore: 80,
    diagnostics: [
      { test: 'touch', status: 'pass', severity: 'critical' },
      { test: 'charging', status: 'pass', severity: 'critical' },
      { test: 'speaker', status: 'fail', severity: 'low' },
    ],
  });
  assert.equal(result.rulesVersion, 'condition-v1');
  assert.ok(result.overallScore >= 0 && result.overallScore <= 100);
  assert.ok(result.functionalScore > 80 && result.functionalScore < 100);
  assert.notEqual(result.recommendedGrade, 'faulty');
});

test('unknown diagnostics reduce confidence and never count as a pass', () => {
  const core = loadCore();
  const complete = core.scoreCondition({
    cosmeticScore: 90,
    diagnostics: [
      { test: 'touch', status: 'pass', severity: 'critical' },
      { test: 'charging', status: 'pass', severity: 'critical' },
    ],
  });
  const partial = core.scoreCondition({
    cosmeticScore: 90,
    diagnostics: [
      { test: 'touch', status: 'pass', severity: 'critical' },
      { test: 'charging', status: 'unknown', severity: 'critical' },
    ],
  });
  assert.ok(partial.confidence < complete.confidence);
  assert.ok(partial.functionalScore < complete.functionalScore);
});

test('critical functional failure caps the recommendation at faulty', () => {
  const core = loadCore();
  const result = core.scoreCondition({
    cosmeticScore: 100,
    diagnostics: [
      { test: 'touch', status: 'fail', severity: 'critical' },
      { test: 'speaker', status: 'pass', severity: 'low' },
    ],
  });
  assert.equal(result.recommendedGrade, 'faulty');
  assert.ok(result.overallScore <= 44);
});

test('stock preparation requires evidence resolution and explicit staff confirmations', () => {
  const core = loadCore();
  const base = {
    identityResolved: true,
    storageResolved: true,
    evidenceSufficient: true,
    proposalReady: true,
    riskReviewResolved: true,
  };
  assert.equal(core.canPrepareStock({ ...base, confirmations: {} }), false);
  assert.equal(core.canPrepareStock({
    ...base,
    confirmations: { grade: true, buyPrice: true, repairDecision: true },
  }), true);
});

test('proposal remains advisory and exposes blockers instead of bypassing them', () => {
  const core = loadCore();
  const proposal = core.buildAssessmentProposal({
    identityResolved: true,
    storageResolved: false,
    evidenceSufficient: true,
    cosmeticScore: 85,
    diagnostics: [],
  });
  assert.equal(proposal.state, 'storage_unresolved');
  assert.equal(proposal.requiresStaffConfirmation, true);
  assert.ok(proposal.blockers.includes('storage_unresolved'));
});
