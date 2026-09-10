import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync(new URL('../supabase/functions/nova-knowledge/index.ts',import.meta.url),'utf8');

test('knowledge summary uses exact head counts instead of capped row arrays',()=>{
  assert.match(source,/select\('id',\{count:'exact',head:true\}\)/);
  assert.match(source,/exactCount\(\{status:'active'\}\)/);
  assert.match(source,/exactCount\(\{status:'archived'\}\)/);
  assert.doesNotMatch(source,/summary\(\)\{const\{data,error\}=await admin\.from\('nova_knowledge_items'\)\.select\('category,trust_level,status'\)/);
});

test('knowledge summary preserves active category and trust breakdowns without a 1000-row fetch',()=>{
  assert.match(source,/Promise\.all\(categories\.map\(async category=>\[category,await exactCount\(\{status:'active',category\}\)\]/);
  assert.match(source,/Promise\.all\(trustLevels\.map\(async trust_level=>\[trust_level,await exactCount\(\{status:'active',trust_level\}\)\]/);
  assert.match(source,/return\{count,active_count,archived_count,by_category,by_trust\}/);
});
