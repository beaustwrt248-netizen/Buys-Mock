import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const novaRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const appPath = path.join(novaRoot, 'app.js');
const workerPath = path.join(novaRoot, 'service-worker.js');
const importPattern = /\bimport\s+(?:[^'\"]*?\s+from\s+)?['\"]([^'\"]+)['\"]/g;

function collectRuntimeModules(entryPath, seen = new Set()) {
  const absolute = path.resolve(entryPath);
  if (seen.has(absolute)) return seen;
  seen.add(absolute);

  const source = fs.readFileSync(absolute, 'utf8');
  for (const match of source.matchAll(importPattern)) {
    const specifier = match[1];
    if (!specifier.startsWith('.')) continue;
    const dependency = path.resolve(path.dirname(absolute), specifier);
    collectRuntimeModules(dependency, seen);
  }
  return seen;
}

function isPrecached(workerSource, relativePath) {
  const normalised = relativePath.split(path.sep).join('/');
  return workerSource.includes(`\${APP_PREFIX}${normalised}`)
    || workerSource.includes(`/nova-next/${normalised}`);
}

test('service worker precaches the complete static module graph used by app.js', () => {
  const worker = fs.readFileSync(workerPath, 'utf8');
  const modules = [...collectRuntimeModules(appPath)]
    .map(file => path.relative(novaRoot, file))
    .filter(file => file !== 'app.js');

  const missing = modules.filter(modulePath => !isPrecached(worker, modulePath));
  assert.deepEqual(missing, [], `missing runtime modules from CORE: ${missing.join(', ')}`);
});

test('service worker only intercepts explicitly precached static paths', () => {
  const worker = fs.readFileSync(workerPath, 'utf8');
  assert.match(worker, /STATIC_PATHS\.has\(url\.pathname\)/);
  assert.doesNotMatch(worker, /cache\.put\(request/);
});

test('app keeps service-worker registration non-fatal', () => {
  const app = fs.readFileSync(appPath, 'utf8');
  assert.match(app, /serviceWorker\.register\(/);
  assert.match(app, /serviceWorker\.register[\s\S]*?\.catch\(\(\) => \{\}\)/);
});
