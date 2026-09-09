import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const api=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaApiClient.java','utf8');
const activity=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionActivity.java','utf8');

test('Android Vision checks only active Morley catalogue rows',()=>{
  assert.match(api,/JSONArray catalogueMatches\(String raw\)/);
  assert.match(api,/active=is\.true/);
  assert.match(api,/search_text=ilike/);
  assert.match(api,/limit=10/);
});

test('model number is preferred for catalogue verification when visible',()=>{
  assert.match(activity,/String modelNumber = assessment\.optString\("model_number"\)/);
  assert.match(activity,/if \(!modelNumber\.isEmpty\(\)\) return modelNumber/);
});

test('catalogue matches remain read-only evidence and can seed pricing identity',()=>{
  assert.match(activity,/Catalogue matches are evidence only; no record is modified from Vision/);
  assert.match(activity,/JSONObject match = matches == null \? null : matches\.optJSONObject\(0\)/);
  assert.match(activity,/formatCatalogueMatches\(matches\)/);
});
