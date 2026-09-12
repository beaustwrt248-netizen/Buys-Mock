(function attachMorleyRestoreCore(root) {
  'use strict';

  const RESTORE_RULES_VERSION = 'restore-v1';
  const SOURCE_SHA_RE = /^[a-f0-9]{40}$/i;
  const SECRET_KEY_RE = /(secret|password|passwd|token|private.?key|service.?role|api.?key|credential)/i;
  const PROTECTED_DATABASE_COMPONENT = 'database';

  function text(value) {
    return typeof value === 'string' ? value.trim() : '';
  }

  function deepFreeze(value) {
    if (!value || typeof value !== 'object' || Object.isFrozen(value)) return value;
    Object.values(value).forEach(deepFreeze);
    return Object.freeze(value);
  }

  function clonePlain(value) {
    if (value == null) return {};
    if (typeof value !== 'object' || Array.isArray(value)) return {};
    return JSON.parse(JSON.stringify(value));
  }

  function containsSecretLikeKey(value) {
    if (!value || typeof value !== 'object') return false;
    if (Array.isArray(value)) return value.some(containsSecretLikeKey);
    return Object.entries(value).some(([key, child]) => SECRET_KEY_RE.test(String(key)) || containsSecretLikeKey(child));
  }

  function normalizeComponents(value) {
    if (!Array.isArray(value)) return [];
    return [...new Set(value.map(text).filter(Boolean))].sort();
  }

  function buildRestoreManifest(input = {}) {
    const manifest = {
      rulesVersion: RESTORE_RULES_VERSION,
      sourceSha: text(input.sourceSha),
      changeRef: text(input.changeRef),
      components: normalizeComponents(input.components),
      releaseRefs: clonePlain(input.releaseRefs),
      webRefs: clonePlain(input.webRefs),
      functionRefs: clonePlain(input.functionRefs),
      migrationHead: text(input.migrationHead),
      configFingerprints: clonePlain(input.configFingerprints),
      createdAt: text(input.createdAt),
    };
    return deepFreeze(manifest);
  }

  function validateRestoreManifest(input = {}) {
    const manifest = buildRestoreManifest(input);
    const errors = [];
    if (!SOURCE_SHA_RE.test(manifest.sourceSha)) errors.push('invalid_source_sha');
    if (!manifest.components.length) errors.push('components_required');
    if (!manifest.changeRef) errors.push('change_ref_required');
    if (!manifest.createdAt || Number.isNaN(Date.parse(manifest.createdAt))) errors.push('created_at_invalid');
    if (containsSecretLikeKey(input)) errors.push('secret_like_field_rejected');
    return deepFreeze({ ok: errors.length === 0, errors, manifest });
  }

  function previewComponentRestore(current = {}, targetInput = {}, requestedComponents = []) {
    const target = buildRestoreManifest(targetInput);
    const requested = normalizeComponents(requestedComponents);
    const blockers = [];
    const unknown = requested.filter(component => component !== PROTECTED_DATABASE_COMPONENT && !target.components.includes(component));
    if (!requested.length) blockers.push('restore_components_required');
    if (unknown.length) blockers.push('restore_component_not_in_target');
    if (requested.includes(PROTECTED_DATABASE_COMPONENT)) blockers.push('database_recovery_requires_protected_plan');
    if (!SOURCE_SHA_RE.test(target.sourceSha)) blockers.push('target_restore_manifest_invalid');

    return deepFreeze({
      allowed: blockers.length === 0,
      rulesVersion: RESTORE_RULES_VERSION,
      currentSourceSha: text(current.sourceSha) || null,
      targetSourceSha: target.sourceSha || null,
      components: requested,
      blockers,
      requiresExplicitApproval: true,
      dataRollbackIncluded: false,
    });
  }

  root.MorleyRestoreCore = Object.freeze({
    version: '1.0.0',
    RESTORE_RULES_VERSION,
    buildRestoreManifest,
    validateRestoreManifest,
    previewComponentRestore,
  });
})(typeof globalThis !== 'undefined' ? globalThis : window);
