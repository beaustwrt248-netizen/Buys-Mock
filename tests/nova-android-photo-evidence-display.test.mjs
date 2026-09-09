import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const helper=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaAndroidOperator.java','utf8');

test('Android Nova Vision renders evidence with its source photo number',()=>{
  assert.match(helper,/appendEvidenceByPhoto\(out, r\.optJSONArray\("evidence_by_photo"\)\)/);
  assert.match(helper,/Evidence by photo:/);
  assert.match(helper,/append\("• Photo "\)\.append\(photo\)/);
});

test('invalid photo indexes are ignored by the native evidence renderer',()=>{
  assert.match(helper,/photo < 1 \|\| photo > MAX_VISION_PHOTOS/);
  assert.match(helper,/evidence == null \|\| evidence\.length\(\) == 0/);
});
