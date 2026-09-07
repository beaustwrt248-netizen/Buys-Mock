import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read=path=>fs.readFileSync(path,'utf8');
const live=read('guardian-live.js');
const readme=read('nova/README.md');
const permissions=read('nova/permissions.js');

test('Nova Security remains a presentation alias over the Guardian live compatibility API',()=>{
  assert.match(live,/const api=\{open,watchTicket:\(id\)=>open\(id\),refresh\};window\.MorleyGuardianLive=api;window\.MorleyNovaSecurity=api;/);
  assert.doesNotMatch(live,/window\.MorleyNovaSecurity\s*=\s*\{[^}]*?(?:approve|execute|repair|bypass|disable)/is);
});

test('Guardian persisted activity and realtime compatibility names remain canonical',()=>{
  assert.match(live,/\/rest\/v1\/guardian_activity\?select=/);
  assert.match(live,/channel\('morley-guardian-user-activity'\)/);
  assert.match(live,/table:'guardian_activity'/);
  assert.match(readme,/Internal `guardian_\*` database objects, repair records and compatibility APIs remain intentionally named Guardian/);
});

test('unified Nova presentation does not grant protected Guardian authority',()=>{
  assert.match(readme,/Guardian remains independently authoritative for protected operations/);
  assert.match(readme,/Nova must not be able to disable, bypass, weaken, self-approve, inherit or silently expand Guardian authority/);
  assert.match(readme,/Guardian incident\/repair approval or execution authority is not granted to Nova/);
  assert.match(readme,/unavailable or incomplete enforcement evidence must never be interpreted as permission/);
  assert.match(permissions,/Guardian is the independent enforcement layer/);
  assert.match(permissions,/cannot self-approve, disable, bypass or execute Guardian work/);
});

test('Guardian live compatibility surface remains read-only',()=>{
  assert.doesNotMatch(live,/\.from\([^)]*\)\.(?:insert|update|delete|upsert)\s*\(/);
  assert.doesNotMatch(live,/\/functions\/v1\//);
  assert.doesNotMatch(live,/\b(?:approve|executeRepair|applyRepair|bypassGuardian|disableGuardian)\b/);
});
