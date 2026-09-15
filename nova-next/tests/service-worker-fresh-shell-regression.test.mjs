import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const worker = fs.readFileSync(path.join(root, 'service-worker.js'), 'utf8');

assert.match(worker, /nova-next-shell-v5/, 'shell cache identity must advance when interaction modules change');
assert.match(worker, /respondWith\(fetch\(request\)/, 'known shell requests must prefer fresh network content');
assert.match(worker, /catch\(async error[\s\S]*caches\.match\(request\)/, 'offline shell requests must retain the install-time cached fallback');
assert.doesNotMatch(worker, /cache\.put\(request/, 'runtime requests must not mutate the immutable offline precache');
assert.doesNotMatch(worker, /caches\.match\(request\)\.then\(hit => hit \|\| fetch\(request\)\)/, 'interactive modules must not remain cache-first indefinitely');

console.log('service-worker-fresh-shell-regression: ok');
