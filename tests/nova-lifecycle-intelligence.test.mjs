import assert from 'node:assert/strict';
import fs from 'node:fs';

const migration=fs.readFileSync('supabase/migrations/20260904170000_inventory_sales_lifecycle.sql','utf8');
const lifecycle=fs.readFileSync('admin/nova-lifecycle-ui.js','utf8');
const bootstrap=fs.readFileSync('admin/nova-auth-bootstrap.js','utf8');
const learning=fs.readFileSync('supabase/functions/nova-learning/index.ts','utf8');

assert.match(migration,/create table if not exists public\.inventory_items/i);
assert.match(migration,/create table if not exists public\.sales_records/i);
assert.match(migration,/alter table public\.inventory_items enable row level security/i);
assert.match(migration,/alter table public\.sales_records enable row level security/i);
assert.match(migration,/private\.is_admin_or_manager\(\)/i);
assert.match(migration,/realised_profit numeric generated always/i);
assert.doesNotMatch(migration,/grant all/i);

assert.match(lifecycle,/id:'inventory\.summary'/);
assert.match(lifecycle,/id:'sales\.summary'/);
assert.match(lifecycle,/id:'lifecycle\.summary'/);
assert.match(lifecycle,/risk:ai\.RISK\.READ/g);
assert.doesNotMatch(lifecycle,/\.insert\(|\.update\(|\.delete\(/);
assert.match(bootstrap,/nova-knowledge-ui\.js\?v=1/);
assert.match(bootstrap,/nova-lifecycle-ui\.js\?v=1/);

assert.match(learning,/from\('inventory_items'\)/);
assert.match(learning,/from\('sales_records'\)/);
assert.match(learning,/source_type:'inventory_items'/);
assert.match(learning,/source_type:'sales_records'/);
assert.match(learning,/confidence:0\.99,verified:true/);
assert.doesNotMatch(learning,/from\('inventory_items'\)\.insert/);
assert.doesNotMatch(learning,/from\('sales_records'\)\.insert/);

console.log('Nova lifecycle intelligence contract passed');
