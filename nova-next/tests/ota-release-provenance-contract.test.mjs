import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const workflow = await readFile(new URL('../../.github/workflows/nova-next-apk-build.yml', import.meta.url), 'utf8');

assert.match(workflow, /gh release create[\s\S]*?--target "\$GITHUB_SHA"/, 'Nova Next release tag must be bound to the exact reviewed/build source SHA');
assert.match(workflow, /--notes "Verified Nova Next Android OTA release\. Built from \$\{GITHUB_SHA\}/, 'release notes must continue recording the exact build SHA');

console.log('ota-release-provenance-contract: ok');
