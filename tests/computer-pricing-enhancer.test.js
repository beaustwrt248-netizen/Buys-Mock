const test=require('node:test');
const assert=require('node:assert/strict');
const {looksLikeUrl,descriptionFromUrl,cleanListingDescription,estimate}=require('../computer-pricing-enhancer.js');

test('Officeworks laptop URL becomes a clean model and spec description',()=>{
  const url='https://www.officeworks.com.au/shop/officeworks/p/lenovo-legion-pro-5-core-i7-24gb-1tb-rtx5060-gaming-laptop-le83nn0063?cm_mmc=Google:SEM:Always_on';
  const clean=descriptionFromUrl(url);
  assert.match(clean,/Lenovo Legion Pro 5 Core i7 24GB 1TB RTX 5060/i);
  assert.doesNotMatch(clean,/officeworks\.com|https?:\/\//i);
});

test('schemeless Officeworks links are detected and cleaned',()=>{
  const url='officeworks.com.au/shop/officeworks/p/lenovo-legion-pro-5-core-i7-24gb-1tb-rtx5060-gaming-laptop-le83nn0063?region_id=GTYP6H';
  assert.equal(looksLikeUrl(url),true);
  const clean=cleanListingDescription(url);
  assert.match(clean,/Lenovo Legion Pro 5 Core i7 24GB 1TB RTX 5060/i);
  assert.doesNotMatch(clean,/officeworks\.com|\/shop\/officeworks|region_id/i);
});

test('URL-only pasted listing never remains the exact model value',()=>{
  const clean=cleanListingDescription('https://example.com/products/dell-xps-15-i7-32gb-1tb-rtx4060-laptop');
  assert.match(clean,/Dell XPS 15 i7 32GB 1TB RTX 4060/i);
  assert.doesNotMatch(clean,/example\.com|\/products\//i);
});

test('estimated buy price stays below max buy and scales with confidence',()=>{
  assert.equal(Math.round(estimate(600,90)),564);
  assert.ok(estimate(600,45)<estimate(600,90));
  assert.ok(estimate(600,90)<=600);
});