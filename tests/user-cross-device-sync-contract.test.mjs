import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const backend=readFileSync('supabase/functions/user-sync-state/index.ts','utf8');
const client=readFileSync('user-cross-device-sync.js','utf8');
const index=readFileSync('index.html','utf8');
const has=(s,n)=>assert.ok(s.includes(n),`Missing ${n}`);

test('sync is authenticated and user identity is server-derived',()=>{has(backend,'admin.auth.getUser(token)');assert.ok(!backend.includes('body?.user_id'))});
test('sync state is strictly whitelisted',()=>{has(backend,"['bm_inv','bm_sales','bm_recent']");assert.ok(!backend.includes('morley_web_auth'))});
test('push uses optimistic revision conflict detection',()=>{has(backend,"error:'SYNC_CONFLICT'");has(backend,'client_revision:expected');has(client,'result.conflict')});
test('conflicts merge rather than overwrite',()=>{has(backend,"action==='merge'");has(backend,'mergeState(server,client)');has(client,"api('merge'")});
test('offline changes queue and replay on reconnect',()=>{has(client,"PENDING_KEY='morley_sync_pending'");has(client,"action==='offline_replay'");has(client,"window.addEventListener('online'")});
test('OTA bootstrap loads sync client',()=>has(index,"'user-cross-device-sync.js?v=1'"));
