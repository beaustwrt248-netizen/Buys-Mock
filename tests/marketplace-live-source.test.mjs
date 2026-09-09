import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {pathToFileURL} from 'node:url';
import {mkdtemp,writeFile} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';

const src=path=>readFile(new URL(`../${path}`,import.meta.url),'utf8');

test('live source parser ignores Gumtree ad ids and requires labelled stock evidence',async()=>{
  const code=await src('supabase/functions/marketplace-reconciliation-source/parser.mjs');
  const dir=await mkdtemp(join(tmpdir(),'morley-market-'));
  const file=join(dir,'parser.mjs');await writeFile(file,code);
  const p=await import(pathToFileURL(file));
  const html='<div>2 listings</div><a href="https://www.gumtree.com.au/s-ad/morley/mobile-phones/iphone-15/1324567890">iPhone 15 256GB</a><a href="https://www.gumtree.com.au/s-ad/morley/mobile-phones/pixel/1324567891">Pixel - Stock No 123456</a>';
  const r=p.parseGumtree(html,'https://www.gumtree.com.au/web/s-user/7667200309546');
  assert.equal(r.total,2);assert.equal(r.rows[0].stockNumber,null);assert.equal(r.rows[1].stockNumber,'123456');
});

test('protected source is admin authenticated and fails closed on partial evidence',async()=>{
  const code=await src('supabase/functions/marketplace-reconciliation-source/index.ts');
  assert.match(code,/role!=='admin'/);
  assert.match(code,/verified|authoritative|confirmed/i);
  assert.match(code,/rows\.length===total/);
  assert.match(code,/state:confirmed\?'confirmed':'unavailable'/);
  assert.doesNotMatch(code,/delete|trash|removeListing|autoDelete/i);
});

test('browser live source clears stale runtime evidence when authoritative snapshot is unavailable',async()=>{
  const code=await src('admin/marketplace-live-source.js');
  assert.match(code,/data\.state!=='confirmed'/);
  assert.match(code,/stock:\[\],listings:\[\]/);
  assert.match(code,/MorleyMarketplaceReconciliation/);
});
