import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync(new URL('../supabase/functions/nova-learning/index.ts',import.meta.url),'utf8');

test('learning harvest and summary paginate through complete source datasets',()=>{
  assert.match(source,/const PAGE = 1000/);
  assert.match(source,/\.range\(from, from \+ PAGE - 1\)/);
  assert.match(source,/if \(page\.length < PAGE\) return rows/);
  assert.match(source,/complete_scan: true/);
});

test('learning endpoint keeps production admin-only boundary',()=>{
  assert.match(source,/p\.role !== 'admin'/);
  assert.match(source,/Admin access required/);
  assert.match(source,/access: 'admin-only'/);
});

test('learning writes are batched instead of one request per experience',()=>{
  assert.match(source,/const UPSERT_BATCH = 200/);
  assert.match(source,/rows\.slice\(i, i \+ UPSERT_BATCH\)\.map\(normaliseLesson\)/);
  assert.match(source,/upsert\(batch, \{ onConflict: 'domain,lesson_key' \}\)/);
});
