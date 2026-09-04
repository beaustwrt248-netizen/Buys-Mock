import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const turnstile=fs.readFileSync('admin/turnstile.html','utf8');
const login=fs.readFileSync('admin/login-security.js','utf8');
const nova=fs.readFileSync('admin/nova-admin-home.js','utf8');

test('Turnstile bootstrap retries instead of hanging forever',()=>{
  assert.match(turnstile,/MAX_API_ATTEMPTS=3/);
  assert.match(turnstile,/bootstrap-error/);
  assert.match(turnstile,/morley_retry=/);
  assert.match(login,/MAX_BOOTSTRAP_RETRIES=2/);
  assert.match(login,/turnstile\.html\?v=3&retry=/);
  assert.match(login,/Security check unavailable\. Tap here to retry\./);
});

test('Nova dashboard enhancer does not mutate unauthenticated sign-in UI',()=>{
  assert.match(nova,/function appReady\(\)/);
  assert.match(nova,/if\(!appReady\(\)\)return/);
});

test('authentication still requires a Turnstile captcha token',()=>{
  assert.match(login,/!captchaToken/);
  assert.match(login,/captchaToken:token/);
  assert.doesNotMatch(login,/captchaToken:\s*['"]?dummy/i);
});
