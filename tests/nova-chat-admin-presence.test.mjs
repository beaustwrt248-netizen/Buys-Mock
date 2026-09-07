import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync('nova/index.html','utf8');
const conversation=fs.readFileSync('nova/conversation.js','utf8');
const core=fs.readFileSync('nova/app-core.js','utf8');
const recommendations=fs.readFileSync('nova/recommendations.js','utf8');
const admin=fs.readFileSync('admin/index.html','utf8');

test('Nova conversation installs only in the authenticated standalone runtime',()=>{
  assert.doesNotMatch(html,/conversation\.js/);
  assert.match(recommendations,/conversation\.js\?v=1/);
  assert.doesNotMatch(admin,/conversation\.js|nova-admin-home/);
});

test('Nova conversational layer supports natural project questions and follow-ups',()=>{
  assert.match(conversation,/function intentFor\(raw\)/);
  assert.match(conversation,/lastIntent/);
  assert.match(conversation,/function answerCatalogue/);
  assert.match(conversation,/function answerSupport/);
});

test('Nova conversation remains read-only and states protected boundaries',()=>{
  assert.doesNotMatch(conversation,/service_role|SUPABASE_SERVICE_ROLE_KEY|\.update\(|\.delete\(|\.insert\(/i);
  assert.match(conversation,/no pricing, auth, Guardian repair, production-write or release authority/);
});
