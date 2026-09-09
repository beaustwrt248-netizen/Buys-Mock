import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const pre=fs.readFileSync('nova/camera-image-preprocessor.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('Camera images are bounded and canvas re-encoded before upload when supported',()=>{
  assert.match(pre,/MAX_EDGE=2560/);
  assert.match(pre,/TARGET_BYTES=2_250_000/);
  assert.match(pre,/createImageBitmap\(file,\{imageOrientation:'from-image'\}\)/);
  assert.match(pre,/canvas\.toBlob/);
  assert.match(pre,/new File\(\[blob\],name,\{type:'image\/jpeg'/);
});

test('preprocessing retries lower quality and smaller dimensions instead of rejecting modern photos immediately',()=>{
  assert.match(pre,/QUALITIES=\[\.88,\.78,\.68\]/);
  assert.match(pre,/draw\(file,2048,\.68\)/);
});

test('preprocessor is scoped to Camera file input and re-dispatches after replacement',()=>{
  assert.match(pre,/input\.id!=='cameraFile'/);
  assert.match(pre,/DataTransfer/);
  assert.match(pre,/input\.dispatchEvent\(new Event\('change',\{bubbles:true\}\)\)/);
});

test('metadata-stripping preprocessor loads before Camera Assistant',()=>{
  const preIndex=loader.indexOf("camera-image-preprocessor.js?v=1");
  const cameraIndex=loader.indexOf("camera-assistant.js?v=2");
  assert.ok(preIndex>=0&&cameraIndex>preIndex);
  assert.match(pre,/strips embedded image metadata/);
});
