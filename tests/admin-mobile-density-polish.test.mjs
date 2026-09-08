import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const css=fs.readFileSync(new URL('../admin/mobile-density-polish.css',import.meta.url),'utf8');
const js=fs.readFileSync(new URL('../admin/mobile-density-polish.js',import.meta.url),'utf8');
const dock=fs.readFileSync(new URL('../admin/mobile-nav-dock.js',import.meta.url),'utf8');

test('mobile density layer protects Android safe area and compacts high-volume lists',()=>{
  assert.match(css,/safe-area-inset-top/);
  assert.match(css,/#pricingList \.pricing-row/);
  assert.match(css,/#ticketsList \.ticket-row/);
  assert.match(css,/#usersList>\.row/);
});

test('exact duplicate pricing presentation is grouped without collapsing distinct variants',()=>{
  assert.match(js,/groupExactPricingDuplicates/);
  assert.match(js,/title,bits,price/);
  assert.match(js,/identical slots grouped/);
  assert.doesNotMatch(js,/delete\(|removeChild\(/);
});

test('activity is human readable while internal event evidence remains available',()=>{
  assert.match(js,/Guardian incident updated/);
  assert.match(js,/Published OTA release updated/);
  assert.match(js,/Internal event:/);
});

test('existing Admin runtime loads the polish assets',()=>{
  assert.match(dock,/mobile-density-polish\.css\?v=1/);
  assert.match(dock,/mobile-density-polish\.js\?v=1/);
});
