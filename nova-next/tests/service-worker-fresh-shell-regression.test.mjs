import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const worker = fs.readFileSync(path.join(root, 'service-worker.js'), 'utf8');

assert.match(worker, /nova-next-shell-v5/, 'shell cache identity must advance when interaction modules change');
assert.match(worker, /fetch\(request\)[\s\S]*cache\.put/, 'static shell requests must refresh from the network when available');
assert.match(worker, /catch[\s\S]*caches\.match\(request\)/, 'offline shell requests must retain a cached fallback');
assert.doesNotMatch(worker, /caches\.match\(request\)\.then\(hit => hit \|\| fetch\(request\)\)/, 'interactive modules must not remain cache-first indefinitely');

console.log('service-worker-fresh-shell-regression: ok');
