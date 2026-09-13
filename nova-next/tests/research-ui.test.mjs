import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const researchUrl = new URL('../src/research-ui.mjs', import.meta.url);
const app = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const sw = fs.readFileSync(new URL('../service-worker.js', import.meta.url), 'utf8');

await test('research UI creates an evidence-first prompt without auto-send', () => {
  assert.equal(fs.existsSync(researchUrl), true, 'research-ui.mjs should exist');
  const ui = fs.readFileSync(researchUrl, 'utf8');
  assert.match(ui, /createResearchUi/);
  assert.match(ui, /sources/i);
  assert.match(ui, /dates/i);
  assert.match(ui, /verified facts/i);
  assert.match(ui, /uncertainty/i);
  assert.match(ui, /conflict/i);
  assert.doesNotMatch(ui, /requestSubmit\(|sendChat\(|sendMessage\(|\.click\(/);
});

await test('Research quick action is runtime-owned and only prefills guarded Chat', () => {
  assert.match(app, /createResearchUi/);
  assert.match(app, /researchUi\.bind\(\)/);
  assert.match(app, /onNavigate: route => setRoute\(route\)/);
});

await test('offline shell precaches the research UI module', () => {
  assert.match(sw, /src\/research-ui\.mjs/);
});

console.log('research-ui: ok');
