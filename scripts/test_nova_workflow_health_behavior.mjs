#!/usr/bin/env node
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync(new URL('../nova/app-core.js', import.meta.url), 'utf8');

function extractFunction(name) {
  const start = source.indexOf(`function ${name}(`);
  assert.notEqual(start, -1, `Missing ${name} implementation`);
  const bodyStart = source.indexOf('{', start);
  let depth = 0;
  for (let index = bodyStart; index < source.length; index += 1) {
    if (source[index] === '{') depth += 1;
    if (source[index] === '}') {
      depth -= 1;
      if (depth === 0) return source.slice(start, index + 1);
    }
  }
  throw new Error(`Could not extract ${name}`);
}

const context = vm.createContext({state: {changedPaths: []}});
vm.runInContext(
  [
    extractFunction('isExpectedSkip'),
    extractFunction('runLabel'),
    extractFunction('runTone'),
    'this.classify = run => ({label: runLabel(run), tone: runTone(run)});',
  ].join('\n'),
  context,
);

function classify(run, changedPaths) {
  context.state.changedPaths = changedPaths;
  return context.classify(run);
}

assert.deepEqual(
  classify({name: 'Auto Publish Morley OTA', status: 'completed', conclusion: 'skipped'}, ['index.html']),
  {label: 'NOT APPLICABLE', tone: 'ok'},
  'web-only changes should classify skipped OTA publication as non-applicable',
);
assert.deepEqual(
  classify({name: 'Nova APK Build', status: 'completed', conclusion: 'skipped'}, ['android/novaapp/Main.kt']),
  {label: 'SKIPPED', tone: 'high'},
  'APK-relevant changes must keep a skipped APK workflow fail-closed',
);
assert.deepEqual(
  classify({name: 'B&L Morley Quality Gate', status: 'completed', conclusion: 'failure'}, ['index.html']),
  {label: 'FAILURE', tone: 'high'},
  'genuine workflow failures must remain blocked',
);
assert.deepEqual(
  classify({name: 'Repository Security Audit', status: 'in_progress', conclusion: null}, ['index.html']),
  {label: 'RUNNING', tone: ''},
  'running workflows must remain incomplete evidence',
);
assert.deepEqual(
  classify({name: 'Repository Security Audit', status: 'completed', conclusion: 'success'}, ['index.html']),
  {label: 'HEALTHY', tone: 'ok'},
  'successful workflows must remain healthy',
);
assert.deepEqual(
  classify({name: 'Repository Security Audit', status: 'completed', conclusion: 'skipped'}, ['index.html']),
  {label: 'SKIPPED', tone: 'high'},
  'unexpected non-release skips must remain fail-closed',
);

console.log('Nova workflow health behavior tests passed');
