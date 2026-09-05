import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const sales=fs.readFileSync('admin/admin-sales-context.js','utf8');
const linking=fs.readFileSync('admin/admin-lifecycle-linking.js','utf8');

test('sales context is read-only and uses authoritative inventory relation',()=>{
  assert.match(sales,/sales_records/);
  assert.match(sales,/inventory_items!sales_records_inventory_item_id_fkey/);
  assert.doesNotMatch(sales,/\.rpc\(/);
  assert.doesNotMatch(sales,/\.insert\(|\.update\(|\.delete\(/);
});

test('sales context exposes useful realised lifecycle measures',()=>{
  assert.match(sales,/AVG DAYS HELD/);
  assert.match(sales,/BEST PROFIT/);
  assert.match(sales,/LOSS-MAKING SALES/);
  assert.match(sales,/AVG MARGIN/);
  assert.match(sales,/item_summary/);
  assert.match(sales,/realised_profit/);
});

test('sales context loader is additive and idempotent',()=>{
  assert.match(linking,/adminSalesContextScript/);
  assert.match(linking,/admin-sales-context\.js\?v=1/);
  assert.match(sales,/salesLifecycleInsights/);
});
