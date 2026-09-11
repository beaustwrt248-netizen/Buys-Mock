(function attachMorleyAssessmentCore(root) {
  'use strict';

  const CONDITION_RULES_VERSION = 'condition-v1';
  const VALUATION_RULES_VERSION = 'valuation-v1';
  const REPAIR_RULES_VERSION = 'repair-v1';
  const RISK_RULES_VERSION = 'risk-v1';
  const PASSPORT_RULES_VERSION = 'passport-v1';
  const VALID_DIAGNOSTIC_STATUSES = new Set(['pass', 'fail', 'unknown', 'not_tested']);
  const SEVERITY_WEIGHTS = Object.freeze({ critical: 4, high: 3, medium: 2, low: 1 });
  const STAFF_DIAGNOSTIC_SEVERITY = Object.freeze({
    display: 'critical', touch: 'critical', camera: 'high', speaker: 'medium', microphone: 'medium',
    charging: 'critical', buttons: 'medium', vibration: 'low', connectivity: 'high',
  });
  const PASSPORT_EVENT_TYPES = new Set([
    'intake','photo','identity','damage_review','diagnostic','condition','valuation','risk_review',
    'override','repair','stock_prepared','stock_movement','markdown','sale',
  ]);

  function clamp(value, min, max) { const number = Number(value); return Number.isFinite(number) ? Math.min(max, Math.max(min, number)) : min; }
  function finiteNumber(value, fallback = 0) { const number = Number(value); return Number.isFinite(number) ? number : fallback; }
  function nonNegative(value, fallback = 0) { return Math.max(0, finiteNumber(value, fallback)); }
  function money(value) { return Math.round(finiteNumber(value, 0) * 100) / 100; }

  function normalizeEvidence(items) {
    if (!Array.isArray(items)) return [];
    return items.map((item) => { const source = item && typeof item === 'object' ? item : {}; return Object.freeze({ ...source, type: String(source.type || 'unknown'), confidence: clamp(source.confidence, 0, 1), verified: source.verified === true }); });
  }

  function resolveAssessmentState(input) {
    const value = input && typeof input === 'object' ? input : {};
    if (value.policyBlocked === true) return 'policy_blocked';
    if (value.identityResolved !== true) return 'identity_unresolved';
    if (value.storageResolved !== true) return 'storage_unresolved';
    if (value.evidenceSufficient !== true) return 'evidence_insufficient';
    if (value.diagnosticsReady !== true) return 'identified';
    if (value.proposalReady !== true) return 'diagnostics_ready';
    if (value.reviewRequired === true || value.riskReviewResolved === false) return 'review_required';
    if (value.staffApproved !== true) return 'proposed';
    if (value.stockPrepared !== true) return 'approved';
    if (value.completed !== true) return 'stock_prepared';
    return 'completed';
  }

  function normalizeDiagnostic(item) {
    const source = item && typeof item === 'object' ? item : {};
    const rawStatus = String(source.status || 'unknown').toLowerCase();
    const rawSeverity = String(source.severity || 'low').toLowerCase();
    return { test: String(source.test || 'unknown'), status: VALID_DIAGNOSTIC_STATUSES.has(rawStatus) ? rawStatus : 'unknown', severity: Object.prototype.hasOwnProperty.call(SEVERITY_WEIGHTS, rawSeverity) ? rawSeverity : 'low' };
  }

  function normalizeStaffDiagnostics(checks) {
    if (!Array.isArray(checks)) return [];
    return checks.map((check) => {
      const source = check && typeof check === 'object' ? check : {};
      const test = String(source.name || source.test || 'unknown').trim().toLowerCase().replace(/\s+/g, '_');
      const rawState = String(source.state || source.status || 'unavailable').trim().toLowerCase();
      const observed = rawState === 'pass' || rawState === 'fail';
      return Object.freeze({ test, status: observed ? rawState : 'not_tested', severity: STAFF_DIAGNOSTIC_SEVERITY[test] || 'low', staffVerified: observed, automatedVerified: observed && source.automated === true && source.platformVerified === true, note: typeof source.note === 'string' && source.note.trim() ? source.note.trim() : null });
    });
  }

  function gradeForScore(score) { if (score >= 90) return 'excellent'; if (score >= 80) return 'good'; if (score >= 65) return 'fair'; if (score >= 45) return 'poor'; return 'faulty'; }

  function scoreCondition(input) {
    const value = input && typeof input === 'object' ? input : {};
    const cosmeticScore = clamp(value.cosmeticScore, 0, 100);
    const diagnostics = Array.isArray(value.diagnostics) ? value.diagnostics.map(normalizeDiagnostic) : [];
    let passWeight = 0, failWeight = 0, totalRelevantWeight = 0, observedWeight = 0, hasCriticalFailure = false;
    diagnostics.forEach((diagnostic) => { const weight = SEVERITY_WEIGHTS[diagnostic.severity]; totalRelevantWeight += weight; if (diagnostic.status === 'pass') { passWeight += weight; observedWeight += weight; } else if (diagnostic.status === 'fail') { failWeight += weight; observedWeight += weight; if (diagnostic.severity === 'critical') hasCriticalFailure = true; } });
    const testedWeight = passWeight + failWeight;
    const testedFunctionalScore = testedWeight > 0 ? (passWeight / testedWeight) * 100 : 0;
    const coverage = totalRelevantWeight > 0 ? observedWeight / totalRelevantWeight : 0;
    const functionalScore = clamp(testedFunctionalScore * coverage, 0, 100);
    let overallScore = clamp((cosmeticScore * 0.45) + (functionalScore * 0.55), 0, 100);
    if (hasCriticalFailure) overallScore = Math.min(overallScore, 44);
    return Object.freeze({ rulesVersion: CONDITION_RULES_VERSION, cosmeticScore: Math.round(cosmeticScore * 100) / 100, functionalScore: Math.round(functionalScore * 100) / 100, overallScore: Math.round(overallScore * 100) / 100, confidence: Math.round(clamp(coverage, 0, 1) * 1000) / 1000, recommendedGrade: hasCriticalFailure ? 'faulty' : gradeForScore(overallScore), criticalFailure: hasCriticalFailure, diagnosticsObserved: diagnostics.filter((item) => item.status === 'pass' || item.status === 'fail').length, diagnosticsTotal: diagnostics.length });
  }

  function scoreStaffCondition(input) { const value = input && typeof input === 'object' ? input : {}; return scoreCondition({ cosmeticScore: value.cosmeticScore, diagnostics: normalizeStaffDiagnostics(value.checks) }); }

  function valuationEvidenceBlockers(value) { const blockers = []; if (value.identityResolved !== true) blockers.push('identity_unresolved'); if (value.storageResolved !== true) blockers.push('storage_unresolved'); if (value.evidenceSufficient !== true) blockers.push('evidence_insufficient'); if (value.policyBlocked === true) blockers.push('policy_blocked'); return blockers; }

  function buildValuationQuote(input) {
    const value = input && typeof input === 'object' ? input : {};
    const blockers = valuationEvidenceBlockers(value), confidence = clamp(value.evidenceConfidence, 0, 1), marketResale = nonNegative(value.marketResale), conditionAdjustment = finiteNumber(value.conditionAdjustment, 0), stockAdjustment = finiteNumber(value.stockAdjustment, 0), demandAdjustment = finiteNumber(value.demandAdjustment, 0), repairCost = nonNegative(value.repairCost), targetMargin = nonNegative(value.targetMargin), minMargin = nonNegative(value.minMargin, targetMargin), maxBuy = value.maxBuy == null ? Number.POSITIVE_INFINITY : nonNegative(value.maxBuy);
    const targetResale = money(Math.max(0, marketResale + conditionAdjustment + stockAdjustment + demandAdjustment));
    const explanation = Object.freeze([`Verified market resale baseline: $${money(marketResale).toFixed(2)}.`,`Condition adjustment: $${money(conditionAdjustment).toFixed(2)}.`,`Stock adjustment: $${money(stockAdjustment).toFixed(2)}.`,`Demand adjustment: $${money(demandAdjustment).toFixed(2)}.`,`Estimated repair cost: $${money(repairCost).toFixed(2)}.`,`Target margin: $${money(targetMargin).toFixed(2)}; hard minimum margin: $${money(minMargin).toFixed(2)}.`]);
    const inputs = Object.freeze({ marketResale: money(marketResale), conditionAdjustment: money(conditionAdjustment), stockAdjustment: money(stockAdjustment), demandAdjustment: money(demandAdjustment), repairCost: money(repairCost), targetMargin: money(targetMargin), minMargin: money(minMargin), maxBuy: Number.isFinite(maxBuy) ? money(maxBuy) : null });
    const base = { targetResale, confidence, inputs, explanation, rulesVersion: VALUATION_RULES_VERSION, commercialAuthority: 'advisory', requiresStaffConfirmation: true };
    if (blockers.length) return Object.freeze({ ...base, state: 'blocked', buyOffer: null, expectedMargin: null, blockers: Object.freeze(blockers), constraints: Object.freeze({ maxBuyApplied: false, minMargin: money(minMargin) }) });
    const hardMarginBuyCeiling = targetResale - repairCost - minMargin;
    if (marketResale <= 0 || hardMarginBuyCeiling < 0) return Object.freeze({ ...base, state: 'review_required', buyOffer: null, expectedMargin: null, blockers: Object.freeze(['minimum_margin_unachievable']), constraints: Object.freeze({ maxBuyApplied: false, minMargin: money(minMargin) }) });
    const targetBuy = Math.max(0, targetResale - repairCost - targetMargin), constrainedBuy = Math.min(targetBuy, hardMarginBuyCeiling, maxBuy), buyOffer = money(constrainedBuy), expectedMargin = money(targetResale - repairCost - buyOffer), maxBuyApplied = Number.isFinite(maxBuy) && maxBuy < Math.min(targetBuy, hardMarginBuyCeiling);
    if (expectedMargin < minMargin) return Object.freeze({ ...base, state: 'review_required', buyOffer: null, expectedMargin, blockers: Object.freeze(['minimum_margin_unachievable']), constraints: Object.freeze({ maxBuyApplied, minMargin: money(minMargin) }) });
    return Object.freeze({ ...base, state: 'proposed', buyOffer, expectedMargin, blockers: Object.freeze([]), constraints: Object.freeze({ maxBuyApplied, minMargin: money(minMargin) }) });
  }

  function decideRepairStrategy(input) {
    const value = input && typeof input === 'object' ? input : {}, buyCost = nonNegative(value.buyCost), resaleAsIs = nonNegative(value.resaleAsIs), resaleAfterRepair = nonNegative(value.resaleAfterRepair), repairCost = nonNegative(value.repairCost), minMargin = nonNegative(value.minMargin), repairDays = nonNegative(value.repairDays), sellThroughDaysAsIs = nonNegative(value.sellThroughDaysAsIs), sellThroughDaysAfterRepair = nonNegative(value.sellThroughDaysAfterRepair), asIsMargin = money(resaleAsIs - buyCost), repairedMargin = money(resaleAfterRepair - buyCost - repairCost);
    let recommendation = 'review_required', reason = 'Neither option clears the required margin; staff review is required.';
    if (repairedMargin >= minMargin && repairedMargin > asIsMargin) { recommendation = 'buy_and_repair'; reason = 'Repair produces the stronger margin while clearing the required minimum.'; } else if (asIsMargin >= minMargin) { recommendation = 'buy_as_is'; reason = 'Selling as-is clears the required margin without repair cost or delay.'; }
    return Object.freeze({ recommendation, reason, asIsMargin, repairedMargin, repairCost: money(repairCost), repairDays, sellThroughDaysAsIs, sellThroughDaysAfterRepair, rulesVersion: REPAIR_RULES_VERSION, commercialAuthority: 'advisory', requiresStaffConfirmation: true });
  }

  function evaluateDealRisk(input) {
    const value = input && typeof input === 'object' ? input : {}, flags = [], identifierRef = typeof value.identifierRef === 'string' && value.identifierRef.trim() ? value.identifierRef.trim() : null;
    const add = (type, severity, explanation, metadata = {}) => flags.push(Object.freeze({ type, severity, explanation, status: 'open', metadata: Object.freeze(metadata) }));
    if (value.duplicateIdentifier === true) add('duplicate_identifier', 'high', 'A protected identifier reference matches another recorded device.', identifierRef ? { identifierRef } : {});
    if (value.identityMismatch === true) add('identity_mismatch', 'high', 'Verified identity evidence conflicts across sources.');
    const deviation = Math.abs(finiteNumber(value.priceDeviationPct, 0));
    if (deviation >= 25) add('price_anomaly', deviation >= 50 ? 'high' : 'medium', `Price differs from the verified reference range by ${money(deviation)}%.`, { deviationPct: money(deviation) });
    if (value.replacedComponentEvidence === true) add('replaced_component', 'medium', 'Evidence suggests a component may have been replaced; staff verification is required.');
    if (value.unusualTransaction === true) add('unusual_transaction', 'medium', 'Transaction characteristics differ from the normal intake pattern and require staff review.');
    const frozenFlags = Object.freeze(flags);
    return Object.freeze({ decision: flags.length ? 'review_required' : 'clear', flags: frozenFlags, requiresStaffReview: flags.length > 0, autoRejected: false, rulesVersion: RISK_RULES_VERSION, commercialAuthority: 'advisory' });
  }

  function buildPassportEvent(input) {
    const value = input && typeof input === 'object' ? input : {}, eventType = String(value.eventType || '').trim().toLowerCase();
    if (!PASSPORT_EVENT_TYPES.has(eventType)) throw new Error('Unsupported Device Passport event type.');
    const sourceDetails = value.details && typeof value.details === 'object' && !Array.isArray(value.details) ? value.details : {};
    return Object.freeze({ eventType, assessmentId: value.assessmentId == null ? null : String(value.assessmentId), details: Object.freeze({ ...sourceDetails }), source: String(value.source || 'staff'), version: value.version == null ? PASSPORT_RULES_VERSION : String(value.version) });
  }

  function canPrepareStock(input) {
    const value = input && typeof input === 'object' ? input : {}, confirmations = value.confirmations && typeof value.confirmations === 'object' ? value.confirmations : {};
    return value.identityResolved === true && value.storageResolved === true && value.evidenceSufficient === true && value.proposalReady === true && value.riskReviewResolved === true && confirmations.grade === true && confirmations.buyPrice === true && confirmations.repairDecision === true;
  }

  function buildAssessmentProposal(input) {
    const value = input && typeof input === 'object' ? input : {}, state = resolveAssessmentState(value), blockers = [];
    if (state === 'identity_unresolved') blockers.push('identity_unresolved'); if (state === 'storage_unresolved') blockers.push('storage_unresolved'); if (state === 'evidence_insufficient') blockers.push('evidence_insufficient'); if (state === 'policy_blocked') blockers.push('policy_blocked'); if (state === 'review_required') blockers.push('review_required');
    return Object.freeze({ state, blockers: Object.freeze(blockers), condition: scoreCondition(value), requiresStaffConfirmation: true, commercialAuthority: 'advisory', rulesVersion: CONDITION_RULES_VERSION });
  }

  const api = Object.freeze({ version: '1.3.0', CONDITION_RULES_VERSION, VALUATION_RULES_VERSION, REPAIR_RULES_VERSION, RISK_RULES_VERSION, PASSPORT_RULES_VERSION, normalizeEvidence, normalizeStaffDiagnostics, resolveAssessmentState, scoreCondition, scoreStaffCondition, buildValuationQuote, decideRepairStrategy, evaluateDealRisk, buildPassportEvent, canPrepareStock, buildAssessmentProposal });
  root.MorleyAssessmentCore = api;
})(typeof globalThis !== 'undefined' ? globalThis : window);
