import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const webSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const adminActivity = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');

test('Admin web starts Turnstile without waiting for credentials', () => {
  assert.match(webSecurity, /loadChallenge\('Security check loading…'\);/);
  assert.match(webSecurity, /turnstile\.html\?v=6&load=/);
  assert.doesNotMatch(webSecurity, /Enter your email and password to begin/);
});

test('Admin native session handoff retries until Supabase and loadSession are ready', () => {
  assert.match(adminActivity, /maxAttempts=120/);
  assert.match(adminActivity, /setTimeout\(install,100\)/);
  assert.match(adminActivity, /typeof window\.loadSession==='function'/);
  assert.match(adminActivity, /setTimeout\(finish,100\)/);
  assert.match(adminActivity, /window\.sb\.auth\.setSession/);
});
