import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import vm from 'node:vm';

const source = readFileSync(new URL('../admin/catalogue-readonly-parity.js', import.meta.url), 'utf8');
const workspace = readFileSync(new URL('../admin/workspace.html', import.meta.url), 'utf8');

test('Admin Catalogue loads every active row across Supabase result pages', async () => {
  const records = Array.from({ length: 1776 }, (_, index) => ({
    id: index + 1,
    category: index < 1200 ? 'mobile_phone' : 'tablet',
    brand: 'Brand',
    model_name: `Model ${index + 1}`,
    model_number: `MODEL-${index + 1}`,
    storage: '128GB',
    active: true,
  }));
  const ranges = [];
  const elements = {
    catalogueList: { innerHTML: '' },
    catalogueStatus: { textContent: '' },
    catalogueSearch: { value: '', addEventListener() {} },
    catalogueCategory: { value: 'all', innerHTML: '', addEventListener() {} },
    catalogueRefreshBtn: { addEventListener() {} },
  };
  const query = {
    select() { return this; },
    eq() { return this; },
    order() { return this; },
    limit(count) { return Promise.resolve({ data: records.slice(0, count), error: null }); },
    range(from, to) {
      ranges.push([from, to]);
      return Promise.resolve({ data: records.slice(from, to + 1), error: null });
    },
  };
  const context = {
    window: {
      __morleyAdminAuthContext: { profile: { role: 'admin' } },
      sb: { from() { return query; } },
    },
    document: {
      readyState: 'loading',
      getElementById(id) { return elements[id] || null; },
      querySelectorAll() { return []; },
      addEventListener() {},
    },
  };
  context.sb = context.window.sb;
  vm.runInNewContext(source, context);

  await context.window.MorleyAdminCatalogueParity.refresh();

  assert.deepEqual(ranges, [[0, 999], [1000, 1999]]);
  assert.match(elements.catalogueStatus.textContent, /Showing 1776 of 1776 active catalogue items/);
  assert.equal((elements.catalogueList.innerHTML.match(/class="row"/g) || []).length, 1776);
  assert.equal(workspace.match(/catalogue-readonly-parity\.js\?v=2/g)?.length, 1);
});
