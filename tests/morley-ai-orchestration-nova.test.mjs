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

const ready = {
  identityResolved: true,
  storageResolved: true,
  evidenceSufficient: true,
  proposalReady: true,
  riskReviewResolved: true,
  confirmations: { grade: true, buyPrice: true, repairDecision: true },
};

test('stock draft remains blocked until evidence, risk review, confirmations and two-photo intake are complete', () => {
  const core = loadCore();
  assert.equal(core.prepareStockDraft({ ...ready, photoCount: 1 }).state, 'blocked');
  assert.equal(core.prepareStockDraft({ ...ready, photoCount: 2, riskReviewResolved: false }).state, 'blocked');
  assert.equal(core.prepareStockDraft({ ...ready, photoCount: 2, confirmations: {} }).state, 'blocked');
});

test('stock preparation creates a draft only and never auto-publishes', () => {
  const core = loadCore();
  const draft = core.prepareStockDraft({
    ...ready,
    photoCount: 2,
    model: 'Pixel 10 Pro',
    modelNumber: 'AU-PIXEL10P',
    storage: '256 GB',
    grade: 'good',
    buyPrice: 420,
    targetResale: 649,
    repairDecision: 'buy_as_is',
    description: 'Google Pixel 10 Pro 256 GB · Good condition',
  });
  assert.equal(draft.state, 'ready_for_staff_publish');
  assert.equal(draft.publishAllowed, false);
  assert.equal(draft.requiresStaffPublish, true);
  assert.equal(draft.stock.model, 'Pixel 10 Pro');
  assert.equal(draft.stock.storage, '256 GB');
  assert.equal(draft.stock.buyPrice, 420);
  assert.equal(draft.stock.targetResale, 649);
});

test('customer assessment summary exposes condition guidance but hides commercial and protected staff data', () => {
  const core = loadCore();
  const summary = core.buildCustomerAssessmentSummary({
    model: 'Pixel 10 Pro',
    storage: '256 GB',
    conditionScore: 86,
    recommendedGrade: 'good',
    confirmedDamageCount: 1,
    pendingDamageCount: 0,
    buyOffer: 420,
    targetResale: 649,
    identifierRef: 'sha256:secret',
    riskFlags: [{ type: 'price_anomaly', severity: 'high' }],
    staffNotes: 'internal only',
  });
  assert.equal(summary.model, 'Pixel 10 Pro');
  assert.equal(summary.conditionScore, 86);
  assert.equal(summary.recommendedGrade, 'good');
  assert.equal(summary.damage.confirmed, 1);
  const json = JSON.stringify(summary);
  assert.doesNotMatch(json, /420|649|sha256:secret|internal only|price_anomaly/);
  assert.equal(summary.commercialRecommendationVisible, false);
});

test('customer assessment summary keeps unavailable condition scores unverified instead of inventing zero', () => {
  const core = loadCore();
  for (const conditionScore of [undefined, null, '', 'pending', false]) {
    const summary = core.buildCustomerAssessmentSummary({ model: 'Pixel 10 Pro', storage: '256 GB', conditionScore });
    assert.equal(summary.conditionScore, null);
    assert.equal(summary.recommendedGrade, 'unverified');
  }
});

test('customer assessment summary never exposes a stale grade when the score is unavailable', () => {
  const core = loadCore();
  for (const conditionScore of [undefined, null, '', 'pending', false]) {
    const summary = core.buildCustomerAssessmentSummary({
      model: 'Pixel 10 Pro',
      storage: '256 GB',
      conditionScore,
      recommendedGrade: 'good',
    });
    assert.equal(summary.conditionScore, null);
    assert.equal(summary.recommendedGrade, 'unverified');
  }
});

test('customer assessment summary preserves valid numeric scores and clamps out-of-range values', () => {
  const core = loadCore();
  assert.equal(core.buildCustomerAssessmentSummary({ conditionScore: '86' }).conditionScore, 86);
  assert.equal(core.buildCustomerAssessmentSummary({ conditionScore: 120 }).conditionScore, 100);
  assert.equal(core.buildCustomerAssessmentSummary({ conditionScore: -5 }).conditionScore, 0);
});

test('Nova planner maps assessment questions to read-only or advisory tools', () => {
  const core = loadCore();
  const buy = core.planNovaAssessmentCommand('What should I buy today?');
  const overpaid = core.planNovaAssessmentCommand('Show purchases where we probably overpaid');
  const repair = core.planNovaAssessmentCommand('Which devices should we repair rather than sell faulty?');
  assert.equal(buy.tool, 'assessment_opportunities');
  assert.equal(buy.classification, 'read_only');
  assert.equal(overpaid.tool, 'overpay_review');
  assert.equal(overpaid.classification, 'read_only');
  assert.equal(repair.tool, 'repair_opportunities');
  assert.equal(repair.classification, 'read_only');
  assert.equal(repair.requiresHumanApproval, false);
});

test('Nova planner keeps publishing and pricing changes approval protected', () => {
  const core = loadCore();
  const publish = core.planNovaAssessmentCommand('Publish this device to stock');
  const price = core.planNovaAssessmentCommand('Change the buy price to 500');
  assert.equal(publish.classification, 'protected');
  assert.equal(price.classification, 'approval_required');
  assert.equal(publish.requiresHumanApproval, true);
  assert.equal(price.requiresHumanApproval, true);
  assert.equal(publish.executeAllowed, false);
  assert.equal(price.executeAllowed, false);
});
