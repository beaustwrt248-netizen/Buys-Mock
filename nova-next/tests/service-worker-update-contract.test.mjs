import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const worker = fs.readFileSync(path.resolve(here, '..', 'service-worker.js'), 'utf8');

assert.match(worker, /const hadPrevious = keys\.some/);
assert.match(worker, /if \(hadPrevious\)/);
assert.match(worker, /NOVA_WEB_UPDATE_READY/);
assert.match(worker, /completion\.css/);
assert.match(worker, /src\/completion-ui\.mjs/);
assert.match(worker, /src\/ota-config\.mjs/);

console.log('service-worker-update-contract: ok');
