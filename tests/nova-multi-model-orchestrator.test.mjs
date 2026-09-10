import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const edge=fs.readFileSync(new URL('../supabase/functions/nova-orchestrator/index.ts',import.meta.url),'utf8');
const client=fs.readFileSync(new URL('../nova/multi-model-conversation.js',import.meta.url),'utf8');
const control=fs.readFileSync(new URL('../nova/control-centre.js',import.meta.url),'utf8');

test('orchestrator requires authenticated enabled admin access',()=>{
  assert.match(edge,/admin\.auth\.getUser\(token\)/);
  assert.match(edge,/profile\.role !== 'admin'/);
  assert.match(edge,/!profile\?\.is_enabled/);
});

test('ensemble spans OpenAI, Google and Anthropic and uses independent fusion',()=>{
  assert.match(edge,/openai\/gpt-5\.6-sol/);
  assert.match(edge,/google\/gemini-3-pro-preview/);
  assert.match(edge,/anthropic\/claude-opus-5/);
  assert.match(edge,/Promise\.allSettled/);
  assert.match(edge,/fusion judge/i);
});

test('protected authority remains advisory',()=>{
  for(const phrase of ['pricing changes','Guardian decisions/repairs','deployments','role/user changes','GitHub merges/releases']) assert.match(edge,new RegExp(phrase.replace(/[\/]/g,'\\/'),'i'));
  assert.match(edge,/Protected actions remain human-gated/i);
});

test('client preserves specialist routes and provides explicit ensemble trigger',()=>{
  assert.match(client,/specialistIntent/);
  assert.match(client,/explicitEnsemble/);
  assert.match(client,/mode=explicitEnsemble\(raw\)\?'ensemble':'auto'/);
  assert.match(client,/NovaAuth\?\.getAccessToken/);
});

test('control centre loads multi-model conversation module',()=>{
  assert.match(control,/multi-model-conversation\.js/);
});
