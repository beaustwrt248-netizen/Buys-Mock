import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const marketSearch = await readFile(new URL('../supabase/functions/market-search-v2/index.ts', import.meta.url), 'utf8');
const laptopGuided = await readFile(new URL('../android/app/src/main/java/com/buysloans/hub/LaptopGuidedScreen.kt', import.meta.url), 'utf8');

test('market search never queries or returns Gumtree', () => {
  assert.doesNotMatch(marketSearch, /gumtree/i);
});

test('laptop guided valuation does not consume or report Gumtree evidence', () => {
  assert.doesNotMatch(laptopGuided, /gumtree/i);
});
