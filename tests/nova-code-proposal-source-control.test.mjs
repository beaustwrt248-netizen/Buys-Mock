import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src=fs.readFileSync('supabase/functions/nova-code-proposal/index.ts','utf8');

test('Nova code proposal remains admin-only and proposal-only',()=>{
  assert.match(src,/getUser\(t\)/);
  assert.match(src,/p\.role!==\'admin\'/);
  assert.match(src,/proposal_only:true/);
  assert.match(src,/tests_run:false/);
  assert.match(src,/protected_paths_rejected:true/);
  assert.match(src,/admin_audit_log/);
});

test('protected source surfaces stay rejected',()=>{
  assert.match(src,/p\.startsWith\('\.github\/'\)/);
  assert.match(src,/p\.startsWith\('supabase\/'\)/);
  assert.match(src,/auth\|security\|guardian\|pricing\|release\|ota/);
  assert.match(src,/androidmanifest\\\.xml/);
  assert.match(src,/build\\\.gradle/);
  assert.match(src,/keystore\|signing/);
  assert.match(src,/nova-actions\|nova-github/);
});

test('model output cannot introduce paths outside supplied current-main files',()=>{
  assert.match(src,/const allowed=new Set\(docs\.map\(x=>x\.path\)\)/);
  assert.match(src,/allowed\.has\(x\.path\)&&safePath\(x\.path\)/);
  assert.match(src,/raw\.githubusercontent\.com\/beaustwrt248-netizen\/Buys-Mock\/main/);
});
