import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../admin/admin-lifecycle-management.js', import.meta.url), 'utf8');

assert.match(
  source,
  /let inventoryRows=\[\],salesRows=\[\],inventoryLoadVersion=0,salesLoadVersion=0;/,
  'inventory and sales reads must keep independent freshness versions',
);

assert.match(
  source,
  /async function loadInventory\(\)\{const requestVersion=\+\+inventoryLoadVersion;const \{data,error\}=await[\s\S]*?if\(requestVersion!==inventoryLoadVersion\)return;if\(error\)/,
  'inventory loads must reject stale successes and failures before updating UI state',
);

assert.match(
  source,
  /async function loadSales\(\)\{const requestVersion=\+\+salesLoadVersion;const \{data,error\}=await[\s\S]*?if\(requestVersion!==salesLoadVersion\)return;if\(error\)/,
  'sales loads must reject stale successes and failures before updating UI state',
);

console.log('Admin lifecycle stale-load regression contract passed.');
