import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const index = fs.readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const login = fs.readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');

test('Admin login never starts Turnstile during initial HTML parsing', () => {
  const frame = index.match(/<iframe id="adminTurnstileFrame"[^>]*>/)?.[0] || '';
  assert.match(frame, /src="about:blank"/);
  assert.match(frame, /loading="lazy"/);
  assert.doesNotMatch(frame, /turnstile\.html/);
  assert.doesNotMatch(frame, /loading="eager"/);
  assert.match(index, /id="challengeStatus"[^>]*>Enter your email and password first\.<\/div>/);
});

test('credential readiness remains the only automatic challenge start path', () => {
  assert.match(login, /if\(credentialsReady\(\)&&!challengeLoaded\)loadChallenge/);
  assert.match(login, /frame\.src='turnstile\.html\?v=4&load='\+Date\.now\(\)/);
  assert.match(login, /loginBtn\.disabled=busy\|\|!captchaToken\|\|!credentialsReady\(\)/);
});
