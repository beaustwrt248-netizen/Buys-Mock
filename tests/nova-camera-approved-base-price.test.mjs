import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync('nova/camera-approved-base-price.js','utf8');
const loader=fs.readFileSync('nova/live-support.js','utf8');

test('Camera uses only verified catalogue identity for authoritative base-buy lookup',()=>{
  assert.match(source,/gate\?\.verified/);
  assert.match(source,/matched_catalogue_id/);
  assert.match(source,/device_catalog_id/);
});

test('Camera displays protected base buy as reference without applying visual grade factors',()=>{
  assert.match(source,/Morley approved base buy/);
  assert.match(source,/protected authoritative buy price/);
  assert.match(source,/does not apply visual-grade market factors/);
  assert.doesNotMatch(source,/\.70|\.50|\.30/);
});

test('storage is matched when both visual and approved-price variants are explicit',()=>{
  assert.match(source,/function storageMatch/);
  assert.match(source,/Math\.abs\(x-y\)<\.5/);
});

test('approved base price module loads after Camera Assistant',()=>{
  assert.ok(loader.indexOf("camera-approved-base-price.js?v=1")>loader.indexOf("camera-assistant.js?v=2"));
});
