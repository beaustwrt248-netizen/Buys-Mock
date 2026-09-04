import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src=fs.readFileSync('admin/user-management-policy.js','utf8');

test('Admin Supabase client avoids navigator.locks deadlock',()=>{
  assert.match(src,/morleyAuthLock/);
  assert.match(src,/authLockQueues=new Map\(\)/);
  assert.match(src,/lock:suppliedAuth\.lock\|\|morleyAuthLock/);
  assert.doesNotMatch(src,/navigator\.locks/);
});

test('Admin auth lock serializes operations without weakening account policy',()=>{
  assert.match(src,/await previous\.catch/);
  assert.match(src,/finally\{release\(\)/);
  assert.match(src,/Only admins can change user accounts/);
  assert.match(src,/Privileged accounts must be demoted before deletion/);
});
