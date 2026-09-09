import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync('nova/camera-live-preview.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('live Camera starts only from explicit getUserMedia action and never requests audio',()=>{
  assert.match(source,/navigator\.mediaDevices\?\.getUserMedia/);
  assert.match(source,/facingMode:\{ideal:'environment'\}/);
  assert.match(source,/audio:false/);
  assert.match(source,/Start live camera/);
});

test('captured frames are still photos routed through existing Camera file input',()=>{
  assert.match(source,/canvas\.toBlob/);
  assert.match(source,/new File\(\[blob\]/);
  assert.match(source,/input\.dispatchEvent\(new Event\('change'/);
  assert.match(source,/captures\.length>=6/);
});

test('stop ends all camera tracks and page exit also stops access',()=>{
  assert.match(source,/for\(const track of stream\.getTracks\(\)\)track\.stop\(\)/);
  assert.match(source,/addEventListener\('pagehide',stop\)/);
  assert.match(source,/addEventListener\('beforeunload',stop\)/);
});

test('live preview loads after Camera Assistant and keeps existing picker fallback',()=>{
  assert.ok(loader.indexOf("camera-live-preview.js?v=1")>loader.indexOf("camera-assistant.js?v=2"));
  assert.match(source,/existing photo picker instead/);
});
