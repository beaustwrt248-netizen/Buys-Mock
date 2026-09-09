import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaAndroidOperator.java','utf8');

test('Nova Android Vision downsizes before full bitmap allocation',()=>{
  assert.match(source,/ImageDecoder\.createSource\(context\.getContentResolver\(\), uri\)/);
  assert.match(source,/decoder\.setTargetSize\(targetWidth, targetHeight\)/);
  assert.match(source,/VISION_LONG_EDGE = 2560/);
  assert.match(source,/VISION_FALLBACK_EDGE = 2048/);
});

test('Nova Android Vision re-encodes photos as JPEG and targets the shared payload budget',()=>{
  assert.match(source,/Bitmap\.CompressFormat\.JPEG/);
  assert.match(source,/TARGET_IMAGE_BYTES = 2_250_000/);
  assert.match(source,/data:image\/jpeg;base64/);
  assert.match(source,/new int\[]\{88, 80, 72, 64, 56, 48\}/);
});

test('Nova Android Vision fails closed when a photo cannot be compressed safely',()=>{
  assert.match(source,/could not be safely compressed for Nova Vision/);
  assert.match(source,/encoded\.length > TARGET_IMAGE_BYTES/);
});

test('native privacy text describes re-encoding and non-persistence',()=>{
  assert.match(source,/re-encoded before upload/);
  assert.match(source,/not stored by Nova Vision/);
});
