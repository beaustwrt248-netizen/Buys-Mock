import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const loginSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const turnstileHtml = readFileSync(new URL('../admin/turnstile.html', import.meta.url), 'utf8');
const indexHtml = readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');

test('browser Admin ships the current auth controller under a fresh cache key', () => {
  assert.match(indexHtml, /login-security\.js\?v=11/);
});

test('browser Admin uses the stable same-origin iframe challenge on desktop and mobile web', () => {
  assert.match(indexHtml, /id="adminTurnstileFrame"/);
  assert.match(loginSecurity, /const frame=document\.getElementById\(['"]adminTurnstileFrame['"]\)/);
  assert.match(loginSecurity, /turnstile\.html\?v=8&browser=1/);
  assert.doesNotMatch(loginSecurity, /adminTurnstileWidget/);
  assert.doesNotMatch(loginSecurity, /window\.turnstile\.render/);
  assert.doesNotMatch(loginSecurity, /function startFallback/);
});

test('browser Admin strictly accepts challenge messages only from its same-origin iframe', () => {
  assert.match(loginSecurity, /event\.source===frame\.contentWindow/);
  assert.match(loginSecurity, /event\.origin===window\.location\.origin/);
  assert.match(loginSecurity, /window\.location\.origin===['"]null['"]&&event\.origin===['"]null['"]/);
  assert.match(loginSecurity, /payload\.source!==['"]morley-turnstile['"]/);
  assert.match(loginSecurity, /payload\.type===['"]token['"]&&payload\.value/);
});

test('browser Admin retries stalled challenge bootstrap and exposes manual recovery', () => {
  assert.match(loginSecurity, /MAX_BOOTSTRAP_RETRIES=2/);
  assert.match(loginSecurity, /bootstrapTimer=setTimeout/);
  assert.match(loginSecurity, /Security check is taking too long\. Retrying…/);
  assert.match(loginSecurity, /Security check unavailable\. Tap here to retry\./);
  assert.match(loginSecurity, /challengeStatus\.addEventListener\(['"]click['"]/);
  assert.match(turnstileHtml, /MAX_API_ATTEMPTS=3/);
  assert.match(turnstileHtml, /apiWatchdog=setTimeout/);
  assert.match(turnstileHtml, /script\.onerror/);
  assert.match(turnstileHtml, /status\.addEventListener\(['"]click['"]/);
});

test('challenge relay repeats completed tokens to survive browser timing races', () => {
  assert.match(turnstileHtml, /post\(type,token\)/);
  assert.match(turnstileHtml, /setTimeout\(\(\)=>post\(type,token\),150\)/);
  assert.match(turnstileHtml, /setTimeout\(\(\)=>post\(type,token\),600\)/);
});

test('browser Admin still requires a completed captcha token for password sign-in', () => {
  assert.match(loginSecurity, /!captchaToken/);
  assert.match(loginSecurity, /captchaToken:token/);
  assert.match(turnstileHtml, /callback:function\(token\)/);
  assert.match(turnstileHtml, /expired-callback/);
  assert.match(turnstileHtml, /error-callback/);
});
