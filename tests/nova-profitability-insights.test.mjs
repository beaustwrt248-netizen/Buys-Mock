import fs from 'node:fs';
import assert from 'node:assert/strict';

const src=fs.readFileSync('admin/nova-profitability-insights.js','utf8');
const bootstrap=fs.readFileSync('admin/nova-auth-bootstrap.js','utf8');

assert.match(src,/profitability\.summary/);
assert.match(src,/sales_records/);
assert.match(src,/inventory_items!sales_records_inventory_item_id_fkey/);
assert.match(src,/realised_profit/);
assert.match(src,/average_holding_days/);
assert.match(src,/best_groups/);
assert.match(src,/worst_groups/);
assert.match(src,/risk:ai\.RISK\.READ/);
assert.doesNotMatch(src,/\.rpc\(/);
assert.doesNotMatch(src,/\.insert\(|\.update\(|\.delete\(/);
assert.match(src,/Profitability insights/);
assert.match(src,/I have not invented a result/);
assert.match(bootstrap,/nova-lifecycle-ui\.js\?v=2','nova-profitability-insights\.js\?v=1','nova-knowledge-ui\.js\?v=2/);
