(function attachMorleyAssessmentCore(root) {
  'use strict';

  const CONDITION_RULES_VERSION = 'condition-v1';
  const VALID_DIAGNOSTIC_STATUSES = new Set(['pass', 'fail', 'unknown', 'not_tested']);
  const SEVERITY_WEIGHTS = Object.freeze({ critical: 4, high: 3, medium: 2, low: 1 });
  const STAFF_DIAGNOSTIC_SEVERITY = Object.freeze({
    display: 'critical',
    touch: 'critical',
    camera: 'high',
    speaker: 'medium',
    microphone: 'medium',
    charging: 'critical',
    buttons: 'medium',
    vibration: 'low',
    connectivity: 'high',
  });

  function clamp(value, min, max) {
    const number = Number(value);
    if (!Number.isFinite(number)) return min;
    return Math.min(max, Math.max(min, number));
  }

  function normalizeEvidence(items) {
    if (!Array.isArray(items)) return [];
    return items.map((item) => {
      const source = item && typeof item === 'object' ? item : {};
      return Object.freeze({
        ...source,
        type: String(source.type || 'unknown'),
        confidence: clamp(source.confidence, 0, 1),
        verified: source.verified === true,
      });
    });
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
    return {
      test: String(source.test || 'unknown'),
      status: VALID_DIAGNOSTIC_STATUSES.has(rawStatus) ? rawStatus : 'unknown',
      severity: Object.prototype.hasOwnProperty.call(SEVERITY_WEIGHTS, rawSeverity) ? rawSeverity : 'low',
    };
  }

  function normalizeStaffDiagnostics(checks) {
    if (!Array.isArray(checks)) return [];
    return checks.map((check) => {
      const source = check && typeof check === 'object' ? check : {};
      const test = String(source.name || source.test || 'unknown')
        .trim()
        .toLowerCase()
        .replace(/\s+/g, '_');
      const rawState = String(source.state || source.status || 'unavailable').trim().toLowerCase();
      const observed = rawState === 'pass' || rawState === 'fail';
      return Object.freeze({
        test,
        status: observed ? rawState : 'not_tested',
        severity: STAFF_DIAGNOSTIC_SEVERITY[test] || 'low',
        staffVerified: observed,
        automatedVerified: observed && source.automated === true && source.platformVerified === true,
        note: typeof source.note === 'string' && source.note.trim() ? source.note.trim() : null,
      });
    });
  }

  function gradeForScore(score) {
    if (score >= 90) return 'excellent';
    if (score >= 80) return 'good';
    if (score >= 65) return 'fair';
    if (score >= 45) return 'poor';
    return 'faulty';
  }

  function scoreCondition(input) {
    const value = input && typeof input === 'object' ? input : {};
    const cosmeticScore = clamp(value.cosmeticScore, 0, 100);
    const diagnostics = Array.isArray(value.diagnostics) ? value.diagnostics.map(normalizeDiagnostic) : [];

    let passWeight = 0;
    let failWeight = 0;
    let totalRelevantWeight = 0;
    let observedWeight = 0;
    let hasCriticalFailure = false;

    diagnostics.forEach((diagnostic) => {
      const weight = SEVERITY_WEIGHTS[diagnostic.severity];
      totalRelevantWeight += weight;
      if (diagnostic.status === 'pass') {
        passWeight += weight;
        observedWeight += weight;
      } else if (diagnostic.status === 'fail') {
        failWeight += weight;
        observedWeight += weight;
        if (diagnostic.severity === 'critical') hasCriticalFailure = true;
      }
    });

    const testedWeight = passWeight + failWeight;
    const testedFunctionalScore = testedWeight > 0 ? (passWeight / testedWeight) * 100 : 0;
    const coverage = totalRelevantWeight > 0 ? observedWeight / totalRelevantWeight : 0;
    const functionalScore = clamp(testedFunctionalScore * coverage, 0, 100);
    let overallScore = clamp((cosmeticScore * 0.45) + (functionalScore * 0.55), 0, 100);

    if (hasCriticalFailure) overallScore = Math.min(overallScore, 44);

    return Object.freeze({
      rulesVersion: CONDITION_RULES_VERSION,
      cosmeticScore: Math.round(cosmeticScore * 100) / 100,
      functionalScore: Math.round(functionalScore * 100) / 100,
      overallScore: Math.round(overallScore * 100) / 100,
      confidence: Math.round(clamp(coverage, 0, 1) * 1000) / 1000,
      recommendedGrade: hasCriticalFailure ? 'faulty' : gradeForScore(overallScore),
      criticalFailure: hasCriticalFailure,
      diagnosticsObserved: diagnostics.filter((item) => item.status === 'pass' || item.status === 'fail').length,
      diagnosticsTotal: diagnostics.length,
    });
  }

  function scoreStaffCondition(input) {
    const value = input && typeof input === 'object' ? input : {};
    return scoreCondition({
      cosmeticScore: value.cosmeticScore,
      diagnostics: normalizeStaffDiagnostics(value.checks),
    });
  }

  function canPrepareStock(input) {
    const value = input && typeof input === 'object' ? input : {};
    const confirmations = value.confirmations && typeof value.confirmations === 'object'
      ? value.confirmations
      : {};

    return value.identityResolved === true
      && value.storageResolved === true
      && value.evidenceSufficient === true
      && value.proposalReady === true
      && value.riskReviewResolved === true
      && confirmations.grade === true
      && confirmations.buyPrice === true
      && confirmations.repairDecision === true;
  }

  function buildAssessmentProposal(input) {
    const value = input && typeof input === 'object' ? input : {};
    const state = resolveAssessmentState(value);
    const blockers = [];
    if (state === 'identity_unresolved') blockers.push('identity_unresolved');
    if (state === 'storage_unresolved') blockers.push('storage_unresolved');
    if (state === 'evidence_insufficient') blockers.push('evidence_insufficient');
    if (state === 'policy_blocked') blockers.push('policy_blocked');
    if (state === 'review_required') blockers.push('review_required');

    return Object.freeze({
      state,
      blockers: Object.freeze(blockers),
      condition: scoreCondition(value),
      requiresStaffConfirmation: true,
      commercialAuthority: 'advisory',
      rulesVersion: CONDITION_RULES_VERSION,
    });
  }

  const api = Object.freeze({
    version: '1.1.0',
    CONDITION_RULES_VERSION,
    normalizeEvidence,
    normalizeStaffDiagnostics,
    resolveAssessmentState,
    scoreCondition,
    scoreStaffCondition,
    canPrepareStock,
    buildAssessmentProposal,
  });

  root.MorleyAssessmentCore = api;
})(typeof globalThis !== 'undefined' ? globalThis : window);
