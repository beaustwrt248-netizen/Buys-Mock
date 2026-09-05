import fs from 'node:fs';
import assert from 'node:assert/strict';

const data = JSON.parse(fs.readFileSync('nova/catalogue-health.json', 'utf8'));

assert.equal(data.snapshot_type, 'atomic_point_in_time', 'catalogue health must identify itself as a point-in-time atomic snapshot');
assert.match(String(data.checked_at || ''), /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/, 'catalogue snapshot must include an exact checked_at timestamp');
assert.match(String(data.catalogue_latest_updated_at || ''), /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/, 'catalogue snapshot must include the latest row-update timestamp observed in the same snapshot');

const categories = data.categories || [];
assert.equal(categories.reduce((sum, row) => sum + Number(row.total || 0), 0), Number(data.total_records), 'category totals must reconcile to total_records');
assert.equal(categories.reduce((sum, row) => sum + Number(row.active || 0), 0), Number(data.active_records), 'category active counts must reconcile to active_records');
assert.equal(categories.reduce((sum, row) => sum + Number(row.missing_model_number || 0), 0), Number(data.missing_model_number), 'category model-number gaps must reconcile to the top-level gap count');
assert.equal(categories.reduce((sum, row) => sum + (row.category === 'wearable' ? 0 : Number(row.missing_storage || 0)), 0), Number(data.missing_storage_actionable), 'actionable storage gaps must reconcile across non-wearable categories');
assert.equal(categories.reduce((sum, row) => sum + Number(row.au_region || 0), 0), Number(data.au_region_records), 'category AU mapping must reconcile to the top-level AU count');

const contamination = data.contamination_by_category || [];
assert.equal(contamination.reduce((sum, row) => sum + Number(row.candidates || 0), 0), Number(data.contamination_candidates), 'contamination candidates must reconcile by category');
assert.equal(contamination.reduce((sum, row) => sum + Number(row.cash_converters || 0), 0), Number(data.contamination_cash_converters), 'Cash Converters contamination candidates must reconcile by category');
assert.equal(contamination.reduce((sum, row) => sum + Number(row.missing_model_number || 0), 0), Number(data.contamination_missing_model_number), 'candidate model-number gaps must reconcile by category');

console.log('nova catalogue atomic snapshot contract: PASS');
