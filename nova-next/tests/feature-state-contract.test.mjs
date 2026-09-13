import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const feature=await readFile(new URL('../src/feature-ui.mjs',import.meta.url),'utf8');
const coordination=await readFile(new URL('../src/coordination-ui.mjs',import.meta.url),'utf8');
const automation=await readFile(new URL('../src/automation-ui.mjs',import.meta.url),'utf8');

test('knowledge and control centre expose loading empty and unavailable truth',()=>{
  assert.match(feature,/Loading current Nova health/);
  assert.match(feature,/No matching trusted knowledge found/);
  assert.match(feature,/Knowledge is temporarily unavailable/);
  assert.match(feature,/Control Centre data is unavailable/);
});

test('coordination and automation never fabricate remote success',()=>{
  assert.match(coordination,/Loading verified status/);
  assert.match(coordination,/temporarily unavailable/);
  assert.match(automation,/no background runner is connected/i);
  assert.doesNotMatch(automation,/executed successfully|scheduled successfully/i);
});
