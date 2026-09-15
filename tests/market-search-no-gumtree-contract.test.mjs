import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const marketSearch = await readFile(new URL('../supabase/functions/market-search-v2/index.ts', import.meta.url), 'utf8');
const laptopGuided = await readFile(new URL('../android/app/src/main/java/com/buysloans/hub/LaptopGuidedScreen.kt', import.meta.url), 'utf8');

const excludedSource = ['gum', 'tree'].join('');
const excludedPattern = new RegExp(excludedSource, 'i');

test('market search excludes the prohibited marketplace source', () => {
  assert.doesNotMatch(marketSearch, excludedPattern);
});

test('laptop guided valuation does not consume or report the prohibited marketplace source', () => {
  assert.doesNotMatch(laptopGuided, excludedPattern);
});
