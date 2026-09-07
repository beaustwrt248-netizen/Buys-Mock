import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const admin=fs.readFileSync('admin/index.html','utf8');
const html=fs.readFileSync('nova/index.html','utf8');
const js=fs.readFileSync('nova/app.js','utf8');
const css=fs.readFileSync('nova/styles.css','utf8');

test('Nova is a standalone protected control centre, not an Admin DOM enhancer',()=>{
  assert.match(html,/Standalone intelligence & development control centre/);
  assert.match(html,/app\.js\?v=2/);
  assert.doesNotMatch(admin,/nova-admin-home|morley-ai\.html/);
  assert.equal(fs.existsSync('admin/nova-admin-home.js'),false);
});

test('standalone Nova preserves protected control ownership',()=>{
  assert.doesNotMatch(js,/service_role|SUPABASE_SERVICE_ROLE_KEY|\.update\(|\.delete\(|\.insert\(/i);
  assert.match(html,/Pricing approvals[\s\S]*Remain protected/);
  assert.match(html,/Release authority[\s\S]*Human gated/);
});

test('standalone Nova navigation remains usable on desktop and mobile',()=>{
  assert.match(html,/aria-label="Nova sections"/);
  assert.match(css,/@media\(max-width:560px\)/);
  assert.match(css,/\.nav\{[^}]*overflow:auto/);
});
