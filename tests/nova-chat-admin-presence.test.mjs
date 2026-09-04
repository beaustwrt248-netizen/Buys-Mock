import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const home=fs.readFileSync('admin/nova-admin-home.js','utf8');
const control=fs.readFileSync('admin/nova-control-centre.js','utf8');
const index=fs.readFileSync('admin/index.html','utf8');

test('Nova Admin presence self-heals after delayed app rendering',()=>{
  assert.match(index,/nova-admin-home\.js/);
  assert.match(home,/setInterval\(\(\)=>\{runEnhancements\(\)/);
  assert.match(home,/MutationObserver\(\(\)=>runEnhancements\(\)\)/);
  assert.match(home,/admin-nova-direct/);
  assert.match(home,/Your AI Command Partner/);
});

test('Nova conversational layer handles greetings before capability fallback',()=>{
  assert.match(control,/Hi! I'm Nova AI/);
  assert.match(control,/casualReply/);
  assert.match(control,/interceptPrompt/);
  assert.match(control,/interceptEnter/);
  assert.match(control,/Nova AI/);
});

test('Nova fixes do not create a protected mutation path',()=>{
  assert.doesNotMatch(home,/service_role|SUPABASE_SERVICE_ROLE_KEY/i);
  assert.doesNotMatch(control,/service_role|SUPABASE_SERVICE_ROLE_KEY/i);
  assert.doesNotMatch(control,/\.update\(|\.delete\(|\.insert\(/);
});
