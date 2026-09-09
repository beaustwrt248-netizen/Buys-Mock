import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const operator=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaAndroidOperator.java','utf8');
const api=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaApiClient.java','utf8');
const activity=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionActivity.java','utf8');

test('Android Nova Vision allows the same six-photo assessment shape as web Nova',()=>{
  assert.match(operator,/MAX_VISION_PHOTOS = 6/);
  assert.match(operator,/Intent\.EXTRA_ALLOW_MULTIPLE/);
  assert.match(api,/put\("image_data_urls", imageDataUrls\)/);
  assert.match(activity,/Math\.min\(clip\.getItemCount\(\), NovaAndroidOperator\.MAX_VISION_PHOTOS\)/);
});

test('Android Vision surfaces condition and guidance fields from the shared backend',()=>{
  for (const field of ['condition_grade','condition_summary','damage_flags','accessories_present','next_photos']) assert.ok(operator.includes(field), `missing ${field}`);
});

test('single-photo callers remain compatible through the array overload',()=>{
  assert.match(api,/JSONObject vision\(String imageDataUrl, String hint\)/);
  assert.match(api,/JSONArray images = new JSONArray\(\)\.put\(imageDataUrl\)/);
  assert.match(api,/return vision\(images, hint\)/);
});
