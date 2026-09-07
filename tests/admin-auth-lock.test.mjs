import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const policy=fs.readFileSync('admin/user-management-policy.js','utf8');
const home=fs.readFileSync('admin/admin-home.js','utf8');

test('Admin account policy does not monkey-patch the Supabase client',()=>{
  assert.doesNotMatch(policy,/morleyAuthLock/);
  assert.doesNotMatch(policy,/api\.createClient=function/);
  assert.match(policy,/Only admins can change user accounts/);
  assert.match(policy,/Privileged accounts must be demoted before deletion/);
});

test('Admin home DOM updates are scheduled and bounded to avoid renderer mutation loops',()=>{
  assert.match(home,/let scheduled=false/);
  assert.match(home,/requestAnimationFrame/);
  assert.match(home,/if\(scheduled\)return/);
  assert.match(home,/if\(\+\+passes>=20\)clearInterval\(timer\)/);
});
