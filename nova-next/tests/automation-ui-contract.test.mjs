import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source=await readFile(new URL('../src/automation-ui.mjs',import.meta.url),'utf8');

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
