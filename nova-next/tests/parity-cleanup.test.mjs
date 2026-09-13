import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const ui = await readFile(new URL('../src/feature-ui.mjs', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');

test('Knowledge More and Help are runtime-owned surfaces, not placeholder-only markup', () => {
  for (const route of ['knowledge', 'more', 'help']) {
    assert.equal(html.includes(`data-route="${route}"><div class="placeholder"`), false, route);
  }
  assert.ok(html.includes('id="novaNextKnowledgeRoot"'));
  assert.ok(html.includes('id="novaNextControlCentreRoot"'));
  assert.ok(html.includes('id="novaNextHelpRoot"'));
});

test('Help exposes diagnostics and safe navigation without privileged actions', () => {
  for (const token of ['renderHelp', "route === 'help'", 'Diagnostics', 'Open Integrations', 'Open Automation', 'Open Control Centre']) {
    assert.ok(ui.includes(token), token);
  }
  for (const forbidden of ['guardian-repair-executor', 'pricing approval', 'release action', 'deploy action']) {
    assert.equal(ui.includes(forbidden), false, forbidden);
  }
});

test('remaining Settings rows are explicit staged actions instead of dead buttons', () => {
  for (const action of ['settings-account', 'settings-appearance', 'settings-notifications', 'settings-privacy', 'settings-about']) {
    assert.ok(html.includes(`data-action="${action}"`), action);
    assert.ok(app.includes(`action === '${action}'`), action);
  }
  assert.ok(app.includes('not connected in Nova Next yet'));
});
