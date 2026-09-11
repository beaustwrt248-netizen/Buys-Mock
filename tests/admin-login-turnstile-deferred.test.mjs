import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const login = fs.readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const turnstile = fs.readFileSync(new URL('../admin/turnstile.html', import.meta.url), 'utf8');

test('Admin login keeps Turnstile out of the WebView until credentials are ready', () => {
  assert.match(login, /frame\.src='about:blank'/);
  assert.match(login, /if\(credentialsReady\(\)&&!challengeLoaded\)loadChallenge/);
  assert.match(login, /Enter your email and password first\./);
  assert.doesNotMatch(login, /MAX_BOOTSTRAP_RETRIES/);
  assert.doesNotMatch(login, /armBootstrapWatchdog/);
});

test('Turnstile initial iframe is inert and has no automatic retry watchdog', () => {
  assert.match(turnstile, /if\(params\.has\('load'\)\|\|params\.has\('retry'\)\)loadApi\(\)/);
  assert.match(turnstile, /else post\('idle',''\)/);
  assert.doesNotMatch(turnstile, /MAX_API_ATTEMPTS/);
  assert.doesNotMatch(turnstile, /apiWatchdog/);
  assert.doesNotMatch(turnstile, /setTimeout\(/);
});

test('Admin sign-in remains fail closed behind a valid CAPTCHA token', () => {
  assert.match(login, /loginBtn\.disabled=busy\|\|!captchaToken\|\|!credentialsReady\(\)/);
  assert.match(login, /if\(!captchaToken\)/);
  assert.match(login, /captchaToken:token/);
});
