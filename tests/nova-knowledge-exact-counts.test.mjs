import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync(new URL('../supabase/functions/nova-knowledge/index.ts',import.meta.url),'utf8');

test('knowledge summary uses exact head counts instead of capped row arrays',()=>{
  assert.match(source,/select\('id',\{count:'exact',head:true\}\)/);
  assert.match(source,/exactCount\(\{status:'active'\}\)/);
  assert.match(source,/exactCount\(\{status:'archived'\}\)/);
  assert.doesNotMatch(source,/select\('category,trust_level,status'\)/);
});

test('knowledge summary preserves active category and trust breakdowns',()=>{
  assert.match(source,/categories\.map\(async category=>\[category,await exactCount\(\{status:'active',category\}\)/);
  assert.match(source,/trustLevels\.map\(async trust_level=>\[trust_level,await exactCount\(\{status:'active',trust_level\}\)/);
  assert.match(source,/return\{count,active_count,archived_count,by_category,by_trust\}/);
});

test('knowledge endpoint retains the production admin-only and no-delete boundaries',()=>{
  assert.match(source,/p\.role!=='admin'/);
  assert.match(source,/Permanent knowledge deletion is disabled through Nova\. Archive instead\./);
  assert.match(source,/access:'admin-only'/);
  assert.doesNotMatch(source,/\['admin','manager'\]\.includes/);
});
