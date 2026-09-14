import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const reportUrl = new URL('../../docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md', import.meta.url);

async function report() {
  return readFile(reportUrl, 'utf8');
}

test('promotion readiness report records the required evidence sections', async () => {
  const text = await report();
  for (const heading of [
    '# Nova Next Promotion Readiness Report',
    '## Feature parity matrix',
    '## Security, privacy and accessibility',
    '## Deployment, cache and mobile evidence',
    '## Decision',
    '## Protected next actions'
  ]) assert.match(text, new RegExp(heading.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
});

test('promotion readiness report covers every required capability group', async () => {
  const text = (await report()).toLowerCase();
  for (const capability of [
    'auth', 'chat', 'knowledge/research', 'vision', 'code proposals', 'files',
    'tasks', 'projects', 'calendar', 'product search', 'voice', 'automation',
    'integrations', 'help/settings', 'android wrapper'
  ]) assert.ok(text.includes(capability), `missing capability: ${capability}`);
});

test('promotion readiness report records immutable release evidence and a decision', async () => {
  const text = await report();
  assert.match(text, /a4c7344756edd7b07ccbcfae88418876f043003e/);
  assert.match(text, /e1be15c2cb11163306afe53b87b8faecd7f2a5799b4c1b05a2cb9661deba1650/);
  assert.match(text, /(?:GO FOR PROMOTION REVIEW|NO-GO)/);
  for (const protectedAction of [
    'production web route replacement',
    'Android production application identity/signing/version compatibility',
    'release/OTA',
    'backend authority expansion'
  ]) assert.ok(text.includes(protectedAction), `missing protected action: ${protectedAction}`);
});
