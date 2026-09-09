import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const api=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaApiClient.java','utf8');

test('Android Vision preflights the same combined image payload ceiling as the backend',()=>{
  assert.match(api,/MAX_VISION_TOTAL_DATA_URL_CHARS = 20_000_000/);
  assert.match(api,/totalChars \+= image\.length\(\)/);
  assert.match(api,/totalChars > MAX_VISION_TOTAL_DATA_URL_CHARS/);
});

test('oversized combined evidence fails before invoking the Vision edge function',()=>{
  const guard=api.indexOf('totalChars > MAX_VISION_TOTAL_DATA_URL_CHARS');
  const invoke=api.indexOf('return edge("nova-vision"');
  assert.ok(guard>=0 && invoke>guard,'combined-size guard must execute before nova-vision network invocation');
  assert.match(api,/combined Vision photos are too large/);
});
