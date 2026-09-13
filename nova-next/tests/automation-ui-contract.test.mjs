import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source=await readFile(new URL('../src/automation-ui.mjs',import.meta.url),'utf8');
const app=await readFile(new URL('../app.js',import.meta.url),'utf8');
const serviceWorker=await readFile(new URL('../service-worker.js',import.meta.url),'utf8');

test('automation UI creates local jobs and discloses execution boundary',()=>{
  assert.match(source,/New Local Job/);
  assert.match(source,/local-only/i);
  assert.match(source,/no background/i);
  assert.doesNotMatch(source,/Run Now|executed successfully|deployed successfully/i);
});

test('automation UI supports task/project links and enable metadata',()=>{
  assert.match(source,/taskId/);
  assert.match(source,/projectId/);
  assert.match(source,/setEnabled/);
});

test('local jobs preserve the existing automation capability status renderer',()=>{
  assert.doesNotMatch(source,/root\.replaceChildren\(\)/);
  assert.match(source,/automation-local-jobs/);
});

test('app and offline shell wire the bounded automation modules',()=>{
  for(const moduleName of ['automation-store.mjs','automation-runtime.mjs','automation-ui.mjs']) {
    assert.ok(app.includes(moduleName), `app missing ${moduleName}`);
    assert.ok(serviceWorker.includes(moduleName), `service worker missing ${moduleName}`);
  }
  assert.match(app,/automationUi\?\.routeChanged/);
});
