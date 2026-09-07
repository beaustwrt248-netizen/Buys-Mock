import fs from 'node:fs';
import assert from 'node:assert/strict';

const sales=fs.readFileSync('admin/admin-sales-context.js','utf8');
const adminHome=fs.readFileSync('admin/admin-home.js','utf8');

assert.match(sales,/sales_records/);
assert.match(sales,/inventory_items!sales_records_inventory_item_id_fkey/);
assert.match(sales,/realised_profit/);
assert.match(sales,/AVG DAYS HELD/);
assert.match(sales,/BEST PROFIT/);
assert.match(sales,/LOSS-MAKING SALES/);
assert.match(sales,/AVG MARGIN/);
assert.doesNotMatch(sales,/\.rpc\(/);
assert.doesNotMatch(sales,/\.insert\(|\.update\(|\.delete\(/);
assert.match(adminHome,/admin-lifecycle-management\.js\?v=1/);
assert.equal(fs.existsSync('admin/nova-profitability-insights.js'),false);
