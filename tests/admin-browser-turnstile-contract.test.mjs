import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const loginSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const indexHtml = readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');

test('browser Admin renders Turnstile directly in the top-level login document for desktop and mobile web', () => {
  assert.match(indexHtml, /id="adminTurnstileFrame"/);
  assert.match(loginSecurity, /document\.createElement\(['"]div['"]\)/);
  assert.match(loginSecurity, /adminTurnstileWidget/);
  assert.match(loginSecurity, /replaceWith\(widget\)/);
  assert.match(loginSecurity, /https:\/\/challenges\.cloudflare\.com\/turnstile\/v0\/api\.js\?render=explicit/);
  assert.match(loginSecurity, /window\.turnstile\.render\(/);
  assert.match(loginSecurity, /sitekey/);
  assert.doesNotMatch(loginSecurity, /postMessage\(/);
  assert.doesNotMatch(loginSecurity, /frame\.contentWindow/);
});

test('browser Admin recovers from blocked or stalled challenge loading instead of hanging forever', () => {
  assert.match(loginSecurity, /challengeWatchdog=setTimeout/);
  assert.match(loginSecurity, /Security check unavailable\. Tap here to retry\./);
  assert.match(loginSecurity, /script\.onerror/);
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
