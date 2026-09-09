import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const activity=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionActivity.java','utf8');

test('camera captures accumulate into one retained Vision evidence session',()=>{
  assert.match(activity,/final ArrayList<Uri> sessionUris = new ArrayList<>\(\)/);
  assert.match(activity,/sessionUris\.add\(pendingCameraUri\)/);
  assert.match(activity,/sessionUris\.size\(\) < NovaAndroidOperator\.MAX_VISION_PHOTOS/);
  assert.match(activity,/analyse\(new ArrayList<>\(sessionUris\)\)/);
});

test('new follow-up evidence invalidates stale valuation and catalogue conclusions',()=>{
  assert.match(activity,/private void evidenceChanged\(\)/);
  assert.match(activity,/lastAssessment = null/);
  assert.match(activity,/lastCatalogueMatches = new JSONArray\(\)/);
  assert.match(activity,/valuation\.setEnabled\(false\)/);
});

test('staff can retain, reanalyse and clear up to six guided photos',()=>{
  assert.match(activity,/Analyse photos/);
  assert.match(activity,/Clear photos/);
  assert.match(activity,/clearEvidenceSession\(\)/);
  assert.match(activity,/Add another useful angle or analyse the full evidence set now/);
});
