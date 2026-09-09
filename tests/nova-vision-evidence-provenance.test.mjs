import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const fn=fs.readFileSync('supabase/functions/nova-vision/index.ts','utf8');

test('Nova Vision requests and returns evidence tied to specific photo indexes',()=>{
  assert.match(fn,/evidence_by_photo/);
  assert.match(fn,/photo_index/);
  assert.match(fn,/1-based photo_index/);
  assert.match(fn,/evidence_by_photo:normaliseEvidenceByPhoto\(parsed\.evidence_by_photo,images\.length\)/);
});

test('per-photo evidence rejects invalid photo indexes and remains bounded',()=>{
  assert.match(fn,/!Number\.isInteger\(index\)\|\|index<1\|\|index>photoCount/);
  assert.match(fn,/textArray\(item\?\.evidence,8,220\)/);
  assert.match(fn,/if\(out\.length>=photoCount\)break/);
});

test('privacy contract still forbids full serial and IMEI output',()=>{
  assert.match(fn,/never include a full serial or IMEI/);
  assert.match(fn,/full_serials_returned:false/);
  assert.match(fn,/label_identifiers:ids/);
});
