import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const turnstile = fs.readFileSync(new URL('../admin/turnstile.html', import.meta.url), 'utf8');
const adminIndex = fs.readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const adminLogin = fs.readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const webAuth = fs.readFileSync(new URL('../web-auth.js', import.meta.url), 'utf8');

test('web auth v4 challenge boots without waiting for the Admin credential bridge', () => {
  assert.match(webAuth, /admin\/turnstile\.html\?v=4/);
  assert.match(turnstile, /params\.get\('v'\)==='4'/);
  assert.match(turnstile, /loadApi\(\)/);
});

test('Admin first parse stays inert and only reveals the challenge after credentials', () => {
  const frame = adminIndex.match(/<iframe id="adminTurnstileFrame"[^>]*>/)?.[0] || '';
  assert.match(frame, /src="about:blank"/);
  assert.match(adminLogin, /setChallengeVisible\(false\)/);
  assert.match(adminLogin, /if\(credentialsReady\(\)&&!challengeLoaded\)loadChallenge/);
  assert.match(adminLogin, /turnstile\.html\?v=4&load=/);
});

test('legacy v2 Admin embeds remain inert for Samsung WebView freeze protection', () => {
  assert.match(turnstile, /params\.has\('load'\)\|\|params\.has\('retry'\)\|\|params\.get\('v'\)==='4'/);
  assert.match(turnstile, /else\{[\s\S]*post\('idle',''\)/);
});
