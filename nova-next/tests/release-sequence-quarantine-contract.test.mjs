import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..', '..');
const workflow = fs.readFileSync(path.join(root, '.github', 'workflows', 'nova-next-apk-build.yml'), 'utf8');

const identityStep = workflow.match(/- name: Resolve guarded Nova Next release identity[\s\S]*?(?=\n\s+- name: Prepare stable signing key)/)?.[0] || '';
assert.ok(identityStep, 'guarded Nova Next release identity step is required');

assert.match(
  identityStep,
  /NOVA_NEXT_QUARANTINED_VERSION_CODES[^\n]*['"]1['"]/,
  'the provenance-conflicted versionCode 1 must be explicitly quarantined rather than silently skipped'
);
assert.match(
  identityStep,
  /while next_code in quarantined_codes:\s*next_code \+= 1/,
  'release sequencing must advance only across explicitly quarantined version codes'
);
assert.match(
  identityStep,
  /current_code != next_code/,
  'arbitrary versionCode skips must remain rejected'
);
assert.doesNotMatch(
  identityStep,
  /current_code\s*>\s*published_code/,
  'release sequencing must not be weakened to accept any merely-greater versionCode'
);

console.log('release-sequence-quarantine-contract: ok');
