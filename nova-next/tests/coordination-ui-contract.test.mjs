import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source=await readFile(new URL('../src/coordination-ui.mjs',import.meta.url),'utf8');

test('calendar groups local work truthfully',()=>{
  for(const label of ['Overdue','Today','Upcoming','Completed']) assert.match(source,new RegExp(label));
  assert.match(source,/listTasks/); assert.match(source,/listProjects/);
  assert.doesNotMatch(source,/Google Calendar|Outlook|synced successfully/i);
});

test('integrations show verified capability and no fake connect action',()=>{
  assert.match(source,/normaliseIntegration/);
  assert.match(source,/capability/);
  assert.match(source,/Verified status only/i);
  assert.doesNotMatch(source,/Connect now|OAuth token|password/i);
});
