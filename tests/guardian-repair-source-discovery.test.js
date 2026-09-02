const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');

const worker = fs.readFileSync('supabase/functions/guardian-repair-worker/index.ts', 'utf8');
const mobile = fs.readFileSync('admin/guardian-mobile-overhaul.css', 'utf8');
const repairs = fs.readFileSync('admin/guardian-repairs.js', 'utf8');

test('Guardian srcdoc incidents discover the generating web sources and regression tests', () => {
  assert.match(worker, /diagnosis_summary/);
  assert.match(worker, /proposed_action/);
  assert.match(worker, /\(\?:about:\)\?srcdoc/);
  for (const path of [
    'web-base.html',
    'web-admin-mode.js',
    'tests/web-admin-srcdoc-regression.test.js',
    'tests/guardian-runtime-regression.test.js',
  ]) assert.ok(worker.includes(`\"${path}\"`), `missing source hint ${path}`);
  assert.ok(worker.includes('p.startsWith("supabase/")'), 'protected Supabase repair boundary must remain');
  assert.ok(worker.includes('p.startsWith(".github/")'), 'workflow repair boundary must remain');
});

test('Guardian mobile audit actions cannot overflow or clip approval controls', () => {
  assert.ok(mobile.includes('min-width:0!important;max-width:100%!important;width:100%!important'));
  assert.ok(mobile.includes('grid-template-columns:minmax(0,1fr)!important'));
  assert.ok(mobile.includes('overflow-wrap:anywhere'));
});

test('Guardian reports missing protected GitHub write configuration explicitly', () => {
  assert.ok(repairs.includes('GITHUB_WRITE_NOT_CONFIGURED'));
  assert.ok(repairs.includes('GUARDIAN_GITHUB_WRITE_TOKEN'));
  assert.ok(repairs.includes('No repository change was made'));
});
