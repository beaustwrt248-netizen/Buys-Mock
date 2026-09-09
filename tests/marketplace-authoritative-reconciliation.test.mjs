import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFile} from 'node:fs/promises';

async function source(path){return readFile(new URL(`../${path}`,import.meta.url),'utf8')}
async function sourceModel(){const code=await source('admin/marketplace-reconciliation-source.js');const sandbox={URL};sandbox.globalThis=sandbox;vm.runInNewContext(code,sandbox);return sandbox.MorleyMarketplaceSource}

test('marketplace source only accepts authoritative Cash Converters and Gumtree URLs',async()=>{
  const m=await sourceModel();
  const s=m.snapshot({checkedAt:'2026-09-09T11:00:00Z',stock:[{itemNumber:'123456',title:'Phone',url:'https://www.cashconverters.com.au/shop/123456'}],listings:[{title:'Stock 123456 phone',url:'https://www.gumtree.com.au/s-ad/example/987654321012'}]});
  assert.equal(s.state,'confirmed');
  assert.equal(s.stock.length,1);
  assert.equal(s.listings.length,1);
  assert.equal(s.listings[0].stockNumber,'123456');
  const bad=m.snapshot({checkedAt:'2026-09-09T11:00:00Z',stock:[{itemNumber:'123456',url:'https://example.com/123456'}],listings:[{url:'https://evil.example/123456'}]});
  assert.equal(bad.state,'unavailable');
  assert.equal(bad.stock.length,0);
  assert.equal(bad.listings.length,0);
});

test('generic numeric extraction remains available only as an explicit helper',async()=>{
  const m=await sourceModel();
  assert.equal(m.stockNumber('Apple iPhone - stock 7654321'),'7654321');
  assert.equal(m.stockNumber('No stock here','Item number 881122'),'881122');
  assert.equal(m.stockNumber('No identifying digits'),null);
});

test('Gumtree ad IDs and unrelated numeric product text are never promoted to listing stock numbers',async()=>{
  const m=await sourceModel();
  const ad=m.listingRow({title:'Samsung Galaxy S24 256GB',description:'Great condition',url:'https://www.gumtree.com.au/s-ad/morley/mobile-phones/samsung-galaxy-s24/987654321012'});
  assert.equal(ad.stockNumber,null);
  const labelled=m.listingRow({title:'Samsung Galaxy S24 256GB',description:'Cashies item number 7654321',url:'https://www.gumtree.com.au/s-ad/morley/mobile-phones/samsung-galaxy-s24/987654321012'});
  assert.equal(labelled.stockNumber,'7654321');
});

test('Cash Converters rows require an explicit or labelled item number',async()=>{
  const m=await sourceModel();
  assert.equal(m.stockRow({title:'iPhone 15 128GB',url:'https://www.cashconverters.com.au/shop/phones-cameras-computers/phones/smartphones/123456789'}).stockNumber,null);
  assert.equal(m.stockRow({itemNumber:'123456',title:'iPhone 15 128GB',url:'https://www.cashconverters.com.au/shop/phones-cameras-computers/phones/smartphones/123456789'}).stockNumber,'123456');
});

test('reconciliation fails closed until both authoritative snapshots exist',async()=>{
  const m=await sourceModel();
  const engine={marketplaceMatch:()=>{throw new Error('must not run')}};
  const result=m.staleCandidates(engine,{checkedAt:'2026-09-09T11:00:00Z',stock:[{itemNumber:'123456',url:'https://www.cashconverters.com.au/shop/123456'}],listings:[]});
  assert.equal(result.state,'unavailable');
  assert.equal(result.rows.length,0);
});

test('stale candidate classification is evidence-only and keeps Gumtree URL',async()=>{
  const m=await sourceModel();
  const engine={marketplaceMatch:(stock,listings)=>[{status:'unmatched_listing',stockNumber:null,stock:null,listing:listings[0]}]};
  const result=m.staleCandidates(engine,{checkedAt:'2026-09-09T11:00:00Z',stock:[{itemNumber:'123456',url:'https://www.cashconverters.com.au/shop/123456'}],listings:[{title:'Old phone listing',url:'https://www.gumtree.com.au/s-ad/example/999999999999'}]});
  assert.equal(result.state,'confirmed');
  assert.equal(result.staleCandidates.length,1);
  assert.match(result.staleCandidates[0].listing.url,/gumtree\.com\.au/);
});

test('browser adapter has no delete semantics and requires both authoritative sources',async()=>{
  const code=await source('admin/marketplace-reconciliation-adapter.js');
  assert.match(code,/authoritativeStock/);
  assert.match(code,/authoritativeListings/);
  assert.match(code,/morley:authoritative-marketplace-snapshot/);
  assert.doesNotMatch(code,/deleteListing|autoDelete|removeListing|trash/);
});
