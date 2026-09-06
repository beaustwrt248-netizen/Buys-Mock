import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const sourcePath = new URL('../android/novaapp/src/main/java/com/buysloans/nova/NovaApiClient.java', import.meta.url);

test('Nova reads the complete live catalogue on every request', async () => {
  const source = await readFile(sourcePath, 'utf8');

  assert.match(source, /JSONArray catalogue\(\)[\s\S]*return getAllPages\(/);
  assert.match(source, /order=id\.asc/);
  assert.match(source, /limit=" \+ pageSize \+ "&offset=" \+ offset/);
  assert.match(source, /offset \+= page\.length\(\)/);
  assert.match(source, /if \(page\.length\(\) < pageSize\) return all/);
  assert.match(source, /connection\.setUseCaches\(false\)/);
  assert.match(source, /Cache-Control", "no-cache, no-store"/);
  assert.doesNotMatch(source, /release_year\.desc\.nullslast&limit=1000/);
});
