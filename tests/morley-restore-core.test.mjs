import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadCore() {
  const source = fs.readFileSync(new URL('../morley-restore-core.js', import.meta.url), 'utf8');
  const window = {};
  const context = vm.createContext({ window, globalThis: window, Object, Array, Number, String, Boolean, Math, Set, Map, JSON, Date });
  vm.runInContext(source, context, { filename: 'morley-restore-core.js' });
  return window.MorleyRestoreCore;
}

const baseManifest = () => ({
  sourceSha: 'b9c33fe61357209848a9dcacdf8c565e723dab8f',
  changeRef: 'PR-1701',
  components: ['morley_buys_android', 'morley_admin_web'],
  releaseRefs: { morleyBuys: 'v2.15.100' },
  webRefs: { admin: 'main@b9c33fe61357209848a9dcacdf8c565e723dab8f' },
  functionRefs: { deviceInspection: 3 },
  migrationHead: '20260911235327',
  configFingerprints: { android: 'sha256:abc123' },
  createdAt: '2026-09-12T13:45:00Z',
});

test('buildRestoreManifest normalizes and freezes an immutable manifest', () => {
  const core = loadCore();
  const result = core.buildRestoreManifest({ ...baseManifest(), components: ['morley_admin_web', 'morley_buys_android', 'morley_admin_web'] });
  assert.deepEqual([...result.components], ['morley_admin_web', 'morley_buys_android']);
  assert.equal(result.sourceSha, baseManifest().sourceSha);
  assert.equal(Object.isFrozen(result), true);
  assert.equal(Object.isFrozen(result.components), true);
});

test('validateRestoreManifest rejects malformed source SHA and empty component sets', () => {
  const core = loadCore();
  assert.equal(core.validateRestoreManifest({ ...baseManifest(), sourceSha: 'not-a-sha' }).ok, false);
  assert.equal(core.validateRestoreManifest({ ...baseManifest(), components: [] }).ok, false);
});

test('restore manifests reject secret-like fields anywhere in metadata', () => {
  const core = loadCore();
  const result = core.validateRestoreManifest({
    ...baseManifest(),
    configFingerprints: { android: 'sha256:abc123', serviceRoleKey: 'do-not-store' },
  });
  assert.equal(result.ok, false);
  assert.ok(result.errors.includes('secret_like_field_rejected'));
});

test('previewComponentRestore only plans requested known components', () => {
  const core = loadCore();
  const target = core.buildRestoreManifest(baseManifest());
  const preview = core.previewComponentRestore(
    { sourceSha: '921010ea57f43bdb37d66ab3ed66deb2ef03fdbd' },
    target,
    ['morley_admin_web'],
  );
  assert.equal(preview.allowed, true);
  assert.deepEqual([...preview.components], ['morley_admin_web']);
  assert.equal(preview.requiresExplicitApproval, true);
});

test('database recovery is always separately protected and never a generic restore action', () => {
  const core = loadCore();
  const target = core.buildRestoreManifest(baseManifest());
  const preview = core.previewComponentRestore({}, target, ['database']);
  assert.equal(preview.allowed, false);
  assert.ok(preview.blockers.includes('database_recovery_requires_protected_plan'));
});
