import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const OLD_MIGRATION = 'supabase/migrations/20260903143000_central_pricing_catalogue.sql';
const CURRENT_MIGRATION = 'supabase/migrations/20260904083000_device_buy_pricing.sql';
const MANIFEST = 'supabase/migration-supersessions.json';
const RECONCILIATION = 'docs/recovery/2026-09-14-central-pricing-migration-supersession.md';

function text(path) {
  return readFileSync(path, 'utf8');
}

function walk(dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const path = join(dir, name);
    const stat = statSync(path);
    if (stat.isDirectory()) out.push(...walk(path));
    else out.push(path);
  }
  return out;
}

test('historical central-pricing migration remains intact but is explicitly superseded', () => {
  assert.ok(existsSync(OLD_MIGRATION), 'historical migration must remain in repository history');
  assert.ok(existsSync(MANIFEST), 'missing explicit migration supersession manifest');
  const manifest = JSON.parse(text(MANIFEST));
  const entry = manifest.supersessions?.find((item) => item.migration === '20260903143000_central_pricing_catalogue.sql');
  assert.ok(entry, 'central pricing migration must have an explicit supersession entry');
  assert.equal(entry.status, 'superseded');
  assert.equal(entry.replaced_by, '20260904083000_device_buy_pricing.sql');
  assert.equal(entry.runtime_authority, 'device_catalog + device_buy_prices');
  assert.equal(entry.production_repair_requires_explicit_approval, true);
  assert.equal(entry.apply_for_environment_parity, false);
});

test('current protected pricing authority is device_catalog plus device_buy_prices', () => {
  const migration = text(CURRENT_MIGRATION);
  const backend = text('supabase/functions/admin-pricing-control/index.ts');
  assert.match(migration, /device_catalog_id bigint not null references public\.device_catalog\(id\)/);
  assert.match(migration, /grant all on public\.device_buy_prices to service_role/);
  assert.match(backend, /from\('device_catalog'\)/);
  assert.match(backend, /from\('device_buy_prices'\)/);
  assert.match(backend, /from\('device_buy_price_history'\)/);
  assert.ok(!backend.includes("from('morley_catalogue_items')"));
  assert.ok(!backend.includes("from('morley_price_history')"));
});

test('active runtime code cannot reintroduce the superseded pricing tables', () => {
  const roots = ['supabase/functions', 'admin', 'android/app/src/main/java/com/buysloans/hub'];
  const forbidden = ['morley_catalogue_items', 'morley_price_history'];
  const matches = [];
  for (const root of roots) {
    for (const path of walk(root)) {
      if (!/\.(?:js|mjs|ts|kt|html)$/.test(path)) continue;
      const body = text(path);
      for (const token of forbidden) if (body.includes(token)) matches.push(`${relative('.', path)}:${token}`);
    }
  }
  assert.deepEqual(matches, [], `superseded pricing authority referenced by runtime code: ${matches.join(', ')}`);
});

test('reconciliation document forbids autonomous production repair and records fresh-bootstrap risk', () => {
  assert.ok(existsSync(RECONCILIATION), 'missing migration reconciliation document');
  const doc = text(RECONCILIATION);
  assert.match(doc, /Fresh-environment behavior/i);
  assert.match(doc, /Do not apply .*central.pricing.* merely to match production migration history/is);
  assert.match(doc, /No production migration-history repair is performed by this change\./);
  assert.match(doc, /explicit approval/i);
  assert.match(doc, /device_catalog.*device_buy_prices/is);
});
