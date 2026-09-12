import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workflow = fs.readFileSync(new URL('../.github/workflows/morley-restore-point.yml', import.meta.url), 'utf8');
const script = fs.readFileSync(new URL('../scripts/build-restore-manifest.mjs', import.meta.url), 'utf8');

test('major pull requests capture the known-good base before deployment', () => {
  assert.match(workflow, /pull_request:/);
  assert.match(workflow, /base_ref/);
  assert.match(workflow, /build-restore-manifest\.mjs/);
  assert.match(workflow, /upload-artifact/);
  assert.match(workflow, /restore-point-/);
});

test('restore capture is read-only and never exposes repository or provider secrets', () => {
  assert.match(workflow, /contents:\s*read/);
  assert.doesNotMatch(workflow, /contents:\s*write/);
  assert.doesNotMatch(workflow, /secrets\./);
  assert.doesNotMatch(workflow, /pull_request_target/);
});

test('manifest generator records source SHA, affected components and no secret material', () => {
  assert.match(script, /sourceSha/);
  assert.match(script, /components/);
  assert.match(script, /migrationHead/);
  assert.match(script, /releaseRefs/);
  assert.match(script, /configFingerprints/);
  assert.doesNotMatch(script, /process\.env\.(?:SUPABASE_SERVICE_ROLE|SERVICE_ROLE|TOKEN|PASSWORD|SECRET)/);
});
