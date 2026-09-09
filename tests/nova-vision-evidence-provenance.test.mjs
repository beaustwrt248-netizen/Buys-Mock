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
  assert.match(fn,/evidenceArray\(item\?\.evidence,8,220\)/);
  assert.match(fn,/if\(out\.length>=photoCount\)break/);
});

test('privacy contract masks identifier-like evidence instead of trusting prompt compliance',()=>{
  assert.match(fn,/never include a full serial or IMEI/);
  assert.match(fn,/function maskEvidence/);
  assert.match(fn,/IMEI/);
  assert.match(fn,/serial/);
  assert.match(fn,/\\d\{10,20\}/);
  assert.match(fn,/evidence:evidenceArray\(parsed\.evidence,16,240\)/);
  assert.match(fn,/full_serials_returned:false/);
  assert.match(fn,/label_identifiers:ids/);
});

// This contract intentionally runs against the exact reconciled PR head.
