import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const backend=readFileSync('supabase/functions/user-security-center/index.ts','utf8');
const client=readFileSync('user-security-center.js','utf8');
const index=readFileSync('index.html','utf8');

const has=(s,n)=>assert.ok(s.includes(n),`Missing ${n}`);

test('session identity comes from verified JWT and session_id claim',()=>{
  has(backend,'admin.auth.getUser(token)');
  has(backend,'session_id');
  assert.ok(!backend.includes('body?.user_id'));
});

test('device registry never stores access or refresh tokens',()=>{
  assert.ok(!/access_token|refresh_token/.test(backend));
  assert.ok(!/access_token|refresh_token/.test(client));
  has(backend,"from('user_session_devices')");
});

test('new sessions generate a security event',()=>{
  has(backend,"event_type: 'new_session'");
  has(client,'d.is_new');
});

test('remote signout revokes other refresh sessions only',()=>{
  has(backend,'/auth/v1/logout?scope=others');
  has(backend,".neq('session_id', sid)");
  has(client,'Sign Out Other Devices');
});

test('normal signout now calls the Supabase local logout endpoint',()=>{
  has(client,'/auth/v1/logout?scope=local');
  has(client,"localStorage.removeItem(STORE)");
});

test('recovery centre is OTA loaded',()=>{
  has(index,"'user-security-center.js?v=1'");
  has(client,'Recovery & Security Centre');
});
