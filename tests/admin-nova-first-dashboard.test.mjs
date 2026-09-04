import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync('admin/index.html','utf8');
const js=fs.readFileSync('admin/nova-admin-home.js','utf8');
const css=fs.readFileSync('admin/nova-admin-home.css','utf8');

test('Admin home loads Nova-first dashboard layer',()=>{
  assert.match(html,/nova-admin-home\.js/);
  assert.match(js,/Your AI Command Partner/);
  assert.match(js,/Chat with Nova/);
  assert.match(js,/href='morley-ai\.html'/);
  assert.match(js,/admin-nova-direct/);
});

test('Nova-first home preserves protected control ownership',()=>{
  assert.doesNotMatch(js,/service_role|SUPABASE_SERVICE_ROLE_KEY|\.update\(|\.delete\(|\.insert\(/i);
  assert.match(js,/clickTab\('pricing'\)/);
  assert.match(js,/clickTab\('controls'\)/);
});

test('mobile dashboard keeps the requested six primary destinations',()=>{
  assert.match(css,/grid-template-columns:repeat\(6,minmax\(0,1fr\)\)!important/);
  assert.match(css,/\[data-tab="release"\]\{display:none!important\}/);
  assert.match(css,/\.admin-nova-direct\{display:flex!important/);
  assert.match(css,/\.nova-quick-grid/);
});
