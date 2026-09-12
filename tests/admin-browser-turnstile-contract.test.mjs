import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const loginSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const indexHtml = readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const turnstileHtml = readFileSync(new URL('../admin/turnstile.html', import.meta.url), 'utf8');

test('browser Admin uses the isolated same-origin Turnstile page instead of injecting Cloudflare into the parent document', () => {
  assert.match(indexHtml, /id="adminTurnstileFrame"/);
  assert.match(loginSecurity, /challengeBase=['"]turnstile\.html\?v=\d+&browser=1['"]/);
  assert.match(loginSecurity, /frame\.src=challengeUrl\(['"]load['"]\)/);
  assert.match(loginSecurity, /event\.source===frame\.contentWindow/);
  assert.match(loginSecurity, /event\.origin===window\.location\.origin/);
  assert.match(loginSecurity, /window\.location\.origin===['"]null['"]&&event\.origin===['"]null['"]/);
  assert.match(loginSecurity, /payload\.source!==['"]morley-turnstile['"]/);
  assert.match(loginSecurity, /payload\.type===['"]token['"]/);
  assert.doesNotMatch(loginSecurity, /challenges\.cloudflare\.com\/turnstile\/v0\/api\.js/);
  assert.doesNotMatch(loginSecurity, /turnstile\.render\(/);
});

test('isolated challenge starts immediately and posts token state to its parent', () => {
  assert.match(turnstileHtml, /loadApi\(\)/);
  assert.match(turnstileHtml, /window\.parent\.postMessage/);
  assert.match(turnstileHtml, /source:'morley-turnstile'/);
});
