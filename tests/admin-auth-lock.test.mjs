import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const policy=fs.readFileSync('admin/user-management-policy.js','utf8');
const nova=fs.readFileSync('admin/nova-admin-home.js','utf8');

test('Admin account policy does not monkey-patch the Supabase client',()=>{
  assert.doesNotMatch(policy,/morleyAuthLock/);
  assert.doesNotMatch(policy,/api\.createClient=function/);
  assert.match(policy,/Only admins can change user accounts/);
  assert.match(policy,/Privileged accounts must be demoted before deletion/);
});

test('Nova Admin DOM updates are idempotent to avoid renderer mutation loops',()=>{
  assert.match(nova,/if\(label\.textContent!==next\)label\.textContent=next/);
  assert.match(nova,/if\(document\.documentElement\.dataset\.novaAdminHome!=='ready'\)/);
  assert.match(nova,/let scheduled=false/);
  assert.match(nova,/requestAnimationFrame/);
});
