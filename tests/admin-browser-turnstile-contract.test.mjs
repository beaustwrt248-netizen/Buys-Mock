import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const loginSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const indexHtml = readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');

test('browser Admin uses direct Turnstile first on desktop and mobile web', () => {
  assert.match(indexHtml, /id="adminTurnstileFrame"/);
  assert.match(loginSecurity, /adminTurnstileWidget/);
  assert.match(loginSecurity, /replaceWith\(widget\)/);
  assert.match(loginSecurity, /https:\/\/challenges\.cloudflare\.com\/turnstile\/v0\/api\.js\?render=explicit/);
  assert.match(loginSecurity, /window\.turnstile\.render\(/);
  assert.match(loginSecurity, /mode=['"]direct['"]/);
});

test('browser Admin automatically falls back to the isolated same-origin challenge if direct Turnstile fails', () => {
  assert.match(loginSecurity, /function startFallback/);
  assert.match(loginSecurity, /turnstile\.html\?v=7&browser=1/);
  assert.match(loginSecurity, /mode=['"]fallback['"]/);
  assert.match(loginSecurity, /event\.source===frame\.contentWindow/);
  assert.match(loginSecurity, /event\.origin===window\.location\.origin/);
  assert.match(loginSecurity, /window\.location\.origin===['"]null['"]&&event\.origin===['"]null['"]/);
  assert.match(loginSecurity, /payload\.source!==['"]morley-turnstile['"]/);
  assert.match(loginSecurity, /payload\.type===['"]token['"]/);
});

test('browser Admin never hangs forever and can retry either transport after mobile lifecycle changes', () => {
  assert.match(loginSecurity, /challengeWatchdog=setTimeout/);
  assert.match(loginSecurity, /Security check unavailable\. Tap here to retry\./);
  assert.match(loginSecurity, /script\.onerror/);
  assert.match(loginSecurity, /function startFallback[\s\S]*?loadingApi=false/);
  assert.match(loginSecurity, /function resetChallenge[\s\S]*?loadingApi=false[\s\S]*?loadApi/);
  assert.match(loginSecurity, /resetChallenge/);
  assert.match(loginSecurity, /visibilitychange/);
  assert.match(loginSecurity, /pageshow/);
});

test('browser Admin still requires a completed captcha token for password sign-in', () => {
  assert.match(loginSecurity, /!captchaToken/);
  assert.match(loginSecurity, /captchaToken:token/);
  assert.match(loginSecurity, /callback:function\(token\)/);
  assert.match(loginSecurity, /expired-callback/);
  assert.match(loginSecurity, /error-callback/);
});
