import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src = fs.readFileSync('supabase/functions/nova-github/index.ts','utf8');

test('Nova GitHub broker remains admin-only and audited',()=>{
  assert.match(src,/getUser\(t\)/);
  assert.match(src,/p\.role!==\'admin\'/);
  assert.match(src,/admin_audit_log/);
  assert.match(src,/nova\.github\.draft_pr/);
});

test('Nova GitHub broker is draft-only and blocks protected authority',()=>{
  assert.match(src,/draft:true/);
  assert.match(src,/Nova cannot merge this PR/);
  assert.match(src,/p\.startsWith\('\.github\/'\)/);
  assert.match(src,/p\.startsWith\('supabase\/'\)/);
  assert.match(src,/auth\|security\|guardian\|pricing\|release\|ota/);
  assert.match(src,/androidmanifest\\\.xml/);
  assert.match(src,/merge','release','deploy','ota','workflow','delete_branch','force_push/);
  assert.doesNotMatch(src,/\/merges\'/);
  assert.doesNotMatch(src,/\/releases\'/);
});

test('Nova GitHub broker stays pinned to the intended repository',()=>{
  assert.match(src,/OWNER='beaustwrt248-netizen',REPO='Buys-Mock'/);
  assert.match(src,/git\/ref\/heads\/main/);
  assert.match(src,/base:'main'/);
});
