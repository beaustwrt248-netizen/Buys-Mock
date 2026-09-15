import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const marketSearch = await readFile(new URL('../supabase/functions/market-search-v2/index.ts', import.meta.url), 'utf8');
const laptopGuided = await readFile(new URL('../android/app/src/main/java/com/buysloans/hub/LaptopGuidedScreen.kt', import.meta.url), 'utf8');
const excludedPattern = new RegExp(String.fromCharCode(103, 117, 109, 116, 114, 101, 101), 'i');

test('market search excludes the prohibited marketplace source', () => {
  assert.doesNotMatch(marketSearch, excludedPattern);
  assert.match(marketSearch, /used:\s*\["ebay",\s*"facebook"\]/);
  assert.match(marketSearch, /braveFacebook\(q, limit\)/);
  assert.match(marketSearch, /serpFacebook\(q\)/);
});

test('laptop guided valuation does not consume or report the prohibited marketplace source', () => {
  assert.doesNotMatch(laptopGuided, excludedPattern);
  assert.match(laptopGuided, /facebookCount\s*=\s*guidedDistinctCandidateCount\(roots,\s*"facebook"\)/s);
  assert.match(laptopGuided, /Facebook \$\{response\.facebookCount\}/);
});
