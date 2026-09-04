import assert from 'node:assert/strict';
import fs from 'node:fs';

const migration=fs.readFileSync('supabase/migrations/20260905021000_admin_lifecycle_operations.sql','utf8');
const ui=fs.readFileSync('admin/admin-lifecycle-management.js','utf8');
const css=fs.readFileSync('admin/admin-lifecycle.css','utf8');
const home=fs.readFileSync('admin/nova-admin-home.js','utf8');
const nova=fs.readFileSync('admin/nova-lifecycle-ui.js','utf8');
const learning=fs.readFileSync('supabase/functions/nova-learning/index.ts','utf8');

for (const fn of ['admin_inventory_create','admin_inventory_set_status','admin_inventory_record_sale']) {
  assert.match(migration,new RegExp(`function public\\.${fn}`,'i'));
}
assert.equal((migration.match(/security definer/gi)||[]).length,3);
assert.equal((migration.match(/private\.is_admin_or_manager\(\)/g)||[]).length,3);
assert.match(migration,/set search_path = public, private, pg_temp/);
assert.match(migration,/Use the sale workflow to mark an item sold/);
assert.match(migration,/for update/gi);
assert.match(migration,/admin_audit_log/);
assert.match(migration,/inventory\.create/);
assert.match(migration,/inventory\.status/);
assert.match(migration,/inventory\.sale/);
assert.match(migration,/revoke all on function public\.admin_inventory_create[\s\S]*from public, anon/i);
assert.match(migration,/grant execute on function public\.admin_inventory_record_sale[\s\S]*to authenticated/i);

assert.match(ui,/admin_inventory_create/);
assert.match(ui,/admin_inventory_set_status/);
assert.match(ui,/admin_inventory_record_sale/);
assert.doesNotMatch(ui,/from\('inventory_items'\)\.insert/);
assert.doesNotMatch(ui,/from\('inventory_items'\)\.update/);
assert.doesNotMatch(ui,/from\('sales_records'\)\.insert/);
assert.match(ui,/confirm\(`Record this item as sold/);
assert.match(ui,/Inventory/);
assert.match(ui,/Sales/);
assert.match(css,/@media\(max-width:520px\)/);
assert.match(home,/admin-lifecycle-management\.js\?v=1/);

for (const source of [nova,learning]) {
  assert.doesNotMatch(source,/admin_inventory_create/);
  assert.doesNotMatch(source,/admin_inventory_set_status/);
  assert.doesNotMatch(source,/admin_inventory_record_sale/);
}

console.log('Admin lifecycle operations contract passed');
