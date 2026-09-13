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

test('desktop search, scanner, reporting and accessibility affordances exist', async () => {
  const source = await read('morley-desktop-rebuild.js');
  assert.match(source, /morleyDesktopGlobalSearch/);
  assert.match(source, /globalSearch\(\)/);
  assert.match(source, /aria-label="Search"/);
  assert.match(source, /aria-label="Notifications"/);
  assert.match(source, /routeMap=.*scanner/s);
  assert.match(source, /text\/csv/);
});
