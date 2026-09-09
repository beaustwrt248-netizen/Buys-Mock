import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const api=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaApiClient.java','utf8');
const activity=fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionActivity.java','utf8');

test('Android Vision uses the authenticated shared market-search-v2 service',()=>{
  assert.match(api,/JSONObject marketSearch\(String query\)/);
  assert.match(api,/edge\("market-search-v2"/);
  assert.match(api,/put\("limit", 30\)/);
});

test('Android Vision applies the same advisory A B C buy factors as web Camera Assistant',()=>{
  assert.match(activity,/"A"\.equals\(grade\) \? \.70/);
  assert.match(activity,/"B"\.equals\(grade\) \? \.50/);
  assert.match(activity,/"C"\.equals\(grade\) \? \.30/);
  assert.match(activity,/manual pricing required/);
  assert.match(activity,/Final price: human approval required/);
});

test('pricing evidence only uses used marketplace groups for the median',()=>{
  assert.match(activity,/market\.optJSONObject\("ebay"\)/);
  assert.match(activity,/market\.optJSONObject\("gumtree"\)/);
  assert.match(activity,/market\.optJSONObject\("facebook"\)/);
  assert.doesNotMatch(activity,/collectPrices\(prices, market\.optJSONObject\("webRetail"\)/);
});

test('valuation remains unavailable until Vision has model identity evidence',()=>{
  assert.match(activity,/valuation\.setEnabled\(hasPricingIdentity\(assessment\)\)/);
  assert.match(activity,/Nova needs a reliable device identity before valuation research/);
});
