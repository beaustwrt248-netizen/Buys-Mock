import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = (path) => readFile(new URL(`../${path}`, import.meta.url), 'utf8');

test('desktop dashboard loader keeps existing runtime and activates rebuild assets', async () => {
  const source = await read('morley-desktop-dashboard.js');
  assert.match(source, /ensureUniversalAssets\(\)/);
  assert.match(source, /morley-desktop-rebuild\.css\?v=20260913/);
  assert.match(source, /morley-desktop-rebuild\.js\?v=20260913/);
  assert.match(source, /ensureCategories\(\)/);
});

test('desktop rebuild exposes all approved primary navigation destinations', async () => {
  const source = await read('morley-desktop-rebuild.js');
  for (const label of ['Dashboard','Search & Scan','Devices','Catalogue','Stock','Sales','Trade In / Buy','Price Check','AI Insights','Reports','Settings']) {
    assert.ok(source.includes(label), `missing desktop nav item: ${label}`);
  }
});

test('desktop quick actions are wired to existing Morley workflows', async () => {
  const source = await read('morley-desktop-rebuild.js');
  for (const action of ['Scan Device','Manual Search','Add New Device','Check Price','View Catalogue','Generate Report']) {
    assert.ok(source.includes(action), `missing quick action: ${action}`);
  }
  assert.match(source, /window\.morleyDesktopGo/);
  assert.match(source, /window\.show/);
  assert.match(source, /#addItem/);
  assert.match(source, /exportReport/);
});

test('desktop rebuild is isolated from physical phone and sub-1000px layouts', async () => {
  const js = await read('morley-desktop-rebuild.js');
  const css = await read('morley-desktop-rebuild.css');
  assert.match(js, /const DESKTOP=1000/);
  assert.match(js, /morley-physical-phone/);
  assert.match(css, /@media \(min-width:1000px\)/);
});

test('desktop resize preserves legacy Morley DOM nodes and runtime listeners', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /function parkLegacyHome/);
  assert.match(source, /legacy\.appendChild\(node\)/);
  assert.match(source, /function restoreLegacyHome/);
  assert.match(source, /home\.insertBefore\(legacy\.firstChild,legacy\)/);
  assert.doesNotMatch(source, /home\.dataset\.morleyOriginal/);
  assert.doesNotMatch(source, /home\.innerHTML=dashboardMarkup/);
});

test('desktop search, scanner, reporting and accessibility affordances exist', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /morleyDesktopGlobalSearch/);
  assert.match(source, /globalSearch\(\)/);
  assert.match(source, /aria-label="Search Morley Buys"/);
  assert.match(source, /aria-label="Search"/);
  assert.match(source, /aria-label="Notifications"/);
  assert.match(source, /aria-label="Open account menu"/);
  assert.match(source, /scanner:/);
  assert.match(source, /text\/csv/);
  assert.match(source, /safeArray/);
});

test('desktop capabilities resolve explicitly instead of masquerading as unrelated routes', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /const capabilities=/);
  assert.match(source, /function resolveCapability/);
  assert.match(source, /function openCapability/);
  for (const name of ['search','scanner','catalogue','inventory','sales','trade','price','ai','reports','settings','notifications','account','support']) {
    assert.ok(source.includes(`${name}:`), `missing capability: ${name}`);
  }
  assert.doesNotMatch(source, /ai:'home'/);
  assert.doesNotMatch(source, /reports:'sales'/);
});

test('search and device categories use truthful desktop capabilities', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /Search & Scan/);
  assert.match(source, /data-mdr-capability="scanner"/);
  assert.match(source, /globalSearch\(\)/);
  assert.match(source, /openCapability\('search'\)/);
  assert.doesNotMatch(source, /\['Tablets','mobilePhones'/);
  assert.doesNotMatch(source, /\['Smartwatches','general'/);
  assert.doesNotMatch(source, /\['Headphones','general'/);
  assert.doesNotMatch(source, /\['Cameras','general'/);
  assert.match(source, /applyCatalogueQuery/);
});

test('desktop shell controls expose real state and explicit behavior', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /function refreshDesktopState/);
  assert.match(source, /MorleyNotifications/);
  assert.match(source, /data-mdr-capability="notifications"/);
  assert.match(source, /data-mdr-capability="account"/);
  assert.match(source, /data-mdr-capability="support"/);
  assert.match(source, /function openInsightsPanel/);
  assert.match(source, /handler:exportReport/);
  assert.doesNotMatch(source, /class="mdr-notify"[^>]*>[\s\S]*?<i>3<\/i>/);
  assert.doesNotMatch(source, /ai:'home'/);
  assert.doesNotMatch(source, /reports:'sales'/);
});

test('desktop dashboard values refresh without replacing parked legacy content', async () => {
  const source = await read('morley-desktop-rebuild.js');
  for (const target of ['units','avg','cost','sold','deals']) assert.ok(source.includes(`data-mdr-kpi=\\"${target}\\"`) || source.includes(`data-mdr-kpi="${target}"`));
  assert.match(source, /data-mdr-market=/);
  assert.match(source, /Current Stock Snapshot/);
  assert.match(source, /refreshDesktopState\(\)/);
  assert.doesNotMatch(source, /home\.innerHTML=dashboardMarkup/);
});

test('desktop lifecycle owns boot and observer state idempotently', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /let booted=false/);
  assert.match(source, /let mainObserver=null/);
  assert.match(source, /if\(booted\)return/);
  assert.match(source, /if\(main&&!mainObserver\)/);
  assert.match(source, /root\.dataset\.mdrBound==='1'/);
});

test('desktop hardening includes visible keyboard focus and insights styling', async () => {
  const css = await read('morley-desktop-rebuild.css');
  assert.match(css, /:focus-visible/);
  assert.match(css, /\.mdr-insights-panel/);
});
