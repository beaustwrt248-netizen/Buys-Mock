import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const linking=fs.readFileSync('admin/admin-lifecycle-linking.js','utf8');
const home=fs.readFileSync('admin/nova-admin-home.js','utf8');
const css=fs.readFileSync('admin/admin-lifecycle-linking.css','utf8');

test('inventory helper only offers quoted valuations and keeps actual buy price explicit',()=>{
  assert.match(linking,/\.eq\('status','quoted'\)/);
  assert.match(linking,/Enter the actual acquired price/);
  assert.doesNotMatch(linking,/invCostInput'\)\.value\s*=\s*valuation\.(max_buy|asking_price)/);
});

test('valuation linking copies identity context without adding write authority',()=>{
  assert.match(linking,/invValuationId/);
  assert.match(linking,/invSummary/);
  assert.match(linking,/invGrade/);
  assert.doesNotMatch(linking,/\.rpc\(/);
  assert.doesNotMatch(linking,/\.insert\(|\.update\(|\.delete\(/);
});

test('helper loads only through authenticated Admin home lifecycle bootstrap',()=>{
  assert.match(home,/admin-lifecycle-management\.js\?v=1/);
  assert.match(home,/admin-lifecycle-linking\.js\?v=1/);
  assert.match(home,/if\(!appReady\(\)\)return/);
});

test('valuation linking remains responsive on phone',()=>{
  assert.match(css,/@media\(max-width:560px\)/);
  assert.match(css,/grid-template-columns:1fr/);
});
