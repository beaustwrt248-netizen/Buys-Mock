import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const webIndex=fs.readFileSync(new URL('../index.html',import.meta.url),'utf8');
const adminIndex=fs.readFileSync(new URL('../admin/index.html',import.meta.url),'utf8');
const adminLogin=fs.readFileSync(new URL('../admin/login-security.js',import.meta.url),'utf8');
const recovery=fs.readFileSync(new URL('../auth-turnstile-cache-recovery.js',import.meta.url),'utf8');

test('Morley web forces fresh auth bootstrap and fresh Turnstile URL',()=>{
  assert.match(webIndex,/web-auth\.js\?v=7/);
  assert.match(webIndex,/auth-turnstile-cache-recovery\.js\?v=1/);
  assert.match(recovery,/admin\/turnstile\.html\?v=5/);
  assert.match(recovery,/web_cache_bust/);
});

test('Morley Admin forces fresh login security and Turnstile URL',()=>{
  assert.match(adminIndex,/login-security\.js\?v=2/);
  assert.match(adminLogin,/turnstile\.html\?v=5&load=/);
  assert.match(adminLogin,/turnstile\.html\?v=5&retry=/);
});
