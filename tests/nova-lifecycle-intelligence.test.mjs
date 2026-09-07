import assert from 'node:assert/strict';
import fs from 'node:fs';

const migration=fs.readFileSync('supabase/migrations/20260905012000_inventory_sales_lifecycle.sql','utf8');
const core=fs.readFileSync('nova/app-core.js','utf8');
const learning=fs.readFileSync('supabase/functions/nova-learning/index.ts','utf8');

assert.match(migration,/create table if not exists public\.inventory_items/i);
assert.match(migration,/create table if not exists public\.sales_records/i);
assert.match(migration,/alter table public\.inventory_items enable row level security/i);
assert.match(migration,/alter table public\.sales_records enable row level security/i);
assert.match(migration,/private\.is_admin_or_manager\(\)/i);
assert.match(migration,/realised_profit numeric generated always/i);
assert.doesNotMatch(migration,/grant all/i);

assert.match(core,/loadMemory\(\)/);
assert.match(core,/learning_experiences/);
assert.match(core,/non-authoritative learned experiences/);
assert.doesNotMatch(core,/admin_inventory_create|admin_inventory_set_status|admin_inventory_record_sale/);

assert.match(learning,/from\('inventory_items'\)/);
assert.match(learning,/from\('sales_records'\)/);
assert.match(learning,/source_type:'inventory_items'/);
assert.match(learning,/source_type:'sales_records'/);
assert.match(learning,/confidence:0\.99,verified:true/);
assert.doesNotMatch(learning,/from\('inventory_items'\)\.insert/);
assert.doesNotMatch(learning,/from\('sales_records'\)\.insert/);

console.log('Nova lifecycle intelligence contract passed');
