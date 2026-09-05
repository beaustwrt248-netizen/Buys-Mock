import fs from 'node:fs';
import assert from 'node:assert/strict';

const data = JSON.parse(fs.readFileSync('nova/catalogue-health.json', 'utf8'));
const report = fs.readFileSync('nova/catalogue-data-quality-report.md', 'utf8');

assert.equal(data.snapshot_type, 'point_in_time', 'catalogue health must identify itself as a point-in-time snapshot');
assert.match(String(data.checked_at || ''), /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/, 'catalogue snapshot must include an exact checked_at timestamp');

const categories = data.categories || [];
assert.equal(categories.reduce((sum, row) => sum + Number(row.total || 0), 0), Number(data.total_records), 'category totals must reconcile to total_records');
assert.equal(categories.reduce((sum, row) => sum + Number(row.active || 0), 0), Number(data.active_records), 'category active counts must reconcile to active_records');
assert.equal(categories.reduce((sum, row) => sum + Number(row.missing_model_number || 0), 0), Number(data.missing_model_number), 'category model-number gaps must reconcile to the top-level gap count');
assert.equal(categories.reduce((sum, row) => sum + (row.category === 'wearable' ? 0 : Number(row.missing_storage || 0)), 0), Number(data.missing_storage_actionable), 'actionable storage gaps must reconcile across non-wearable categories');
assert.equal(categories.reduce((sum, row) => sum + Number(row.au_region || 0), 0), Number(data.au_region_records), 'category AU counts must reconcile to au_region_records');

for (const value of ['1,320', '1,302', '1,002', '165', '37']) {
  assert.match(report, new RegExp(value.replace(',', '\\,')), `report must include current snapshot value ${value}`);
}
assert.doesNotMatch(report, /Total records:\s*1,317|Active records:\s*1,299|AU\/Australia-mapped records:\s*1,060|111 active records/, 'report must not retain stale snapshot values');
assert.match(report, /point-in-time snapshot/i, 'report must identify the evidence as point in time');
assert.match(report, /catalogue_import_validator\.py/, 'report must reference the import validator');
assert.match(report, /catalogue_cleanup_audit\.py/, 'report must reference the cleanup audit tool');

console.log('nova catalogue snapshot consistency contract: PASS');
