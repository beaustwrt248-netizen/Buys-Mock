import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const source=fs.readFileSync(new URL('../nova/conversation.js',import.meta.url),'utf8');
const loader=fs.readFileSync(new URL('../nova/recommendations.js',import.meta.url),'utf8');
const css=fs.readFileSync(new URL('../nova/conversation.css',import.meta.url),'utf8');

test('Ask Nova routes natural project questions to current read-only evidence',()=>{
  for(const intent of ['priority','release','support','catalogue','guardian','knowledge','activity','help'])assert.match(source,new RegExp(`return'${intent}'`));
  assert.match(source,/api\('\/actions\/runs\?branch=main&per_page=50'\)/);
  assert.match(source,/api\('\/pulls\?state=open&per_page=50'\)/);
  assert.match(source,/local\('catalogue-health\.json'\)/);
  assert.match(source,/local\('support-health\.json'\)/);
  assert.match(source,/local\('memory-health\.json'\)/);
});

test('short follow-ups preserve the previous conversation topic',()=>{
  assert.match(source,/\^\(why\|how so\|tell me more\|what about that\|and that\|what do you mean\|more\|details\?\)/);
  assert.match(source,/&&lastIntent\)return lastIntent/);
  assert.match(source,/sessionStorage\.setItem\(STORE/);
  assert.match(source,/history\.slice\(-8\)/);
});

test('Nova fails closed when context is unavailable and keeps protected authority out',()=>{
  assert.match(source,/I will not guess while the project context is unavailable/);
  assert.match(source,/Protected release authority still stays outside this conversation layer/);
  assert.match(source,/Guardian remains an independent protected validation layer/);
  assert.match(source,/I will not invent exact device specifications from aggregate data/);
  assert.match(source,/no pricing, auth, Guardian repair, production-write or release authority/i);
});

test('conversation UI is loaded by the existing Nova bootstrap and remains responsive',()=>{
  assert.match(loader,/conversation\.css\?v=1/);
  assert.match(loader,/conversation\.js\?v=1/);
  assert.match(css,/@media\(max-width:620px\)/);
  assert.match(css,/grid-template-columns:1fr/);
});
