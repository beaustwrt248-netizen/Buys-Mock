import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..', '..');
const workflow = fs.readFileSync(path.join(root, '.github', 'workflows', 'nova-next-apk-build.yml'), 'utf8');

const releaseStep = workflow.match(/- name: Publish immutable Nova Next GitHub Release[\s\S]*?(?=\n\s+- name: Stage guarded Nova Next OTA metadata)/)?.[0] || '';
assert.ok(releaseStep, 'Nova Next immutable release step is required');
assert.match(releaseStep, /gh release create "\$NOVA_NEXT_RELEASE_TAG"/);
assert.match(
  releaseStep,
  /--target "\$GITHUB_SHA"/,
  'Nova Next release tag must explicitly target the exact source SHA that built the signed APK'
);
assert.match(releaseStep, /already exists; refusing version identity reuse/);

console.log('release-provenance-contract: ok');
