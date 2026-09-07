import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync('admin/control-centre-visual.js','utf8');

test('approved Admin dashboard uses real existing evidence instead of decorative live values',()=>{
  assert.match(source,/deviceVersionAdoption/);
  assert.match(source,/deviceAdoptionSummary/);
  assert.match(source,/auditList/);
  assert.match(source,/MutationObserver/);
  assert.match(source,/--admin-release-percent/);
  assert.match(source,/content:attr\(data-label\)/);
  assert.doesNotMatch(source,/Sync Listings/);
  assert.doesNotMatch(source,/Catalogue intelligence connected/);
  assert.doesNotMatch(source,/Protected pricing controls ready/);
  assert.doesNotMatch(source,/Support and release monitoring active/);
  assert.doesNotMatch(source,/height:44%/);
  assert.doesNotMatch(source,/height:91%/);
  assert.doesNotMatch(source,/Live catalogue and listing services/);
});

test('Admin overview KPI labels still match the authoritative metric sources',()=>{
  assert.match(source,/Users & Access/);
  assert.match(source,/Registered Devices/);
  assert.match(source,/Current Release/);
  assert.match(source,/Queued Notifications/);
});
