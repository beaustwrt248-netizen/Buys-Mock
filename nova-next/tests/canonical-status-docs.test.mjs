import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

test('canonical Nova Next status records the verified completion state', async () => {
  const status = await read('docs/nova-next/STATUS.md');
  assert.match(status, /GO FOR PROMOTION REVIEW/);
  assert.match(status, /UI, launcher and isolated OTA/i);
  assert.match(status, /manual[^\n]*OTA|OTA[^\n]*manual/i);
  assert.match(status, /production[^\n]*(?:not|remain|protected)/i);
  assert.doesNotMatch(status, /Bootstrap Status/);
});

test('canonical feature parity map no longer labels verified capabilities registry-only', async () => {
  const parity = await read('docs/nova-next/FEATURE_PARITY.md');
  assert.match(parity, /ready-with-boundary/);
  assert.match(parity, /Conversation \/ AI chat[^\n]*ready/i);
  assert.match(parity, /Voice assistant[^\n]*ready-with-boundary/i);
  assert.match(parity, /Live web research[^\n]*ready-with-boundary/i);
  assert.match(parity, /OTA[^\n]*manual|manual[^\n]*OTA/i);
  assert.doesNotMatch(parity, /Registry \+ UI shell/);
});

test('visual validation record matches the completed promotion-review state', async () => {
  const visual = await read('docs/nova-next/VISUAL_VALIDATION.md');
  assert.match(visual, /GO FOR PROMOTION REVIEW/);
  assert.match(visual, /mobile[^\n]*(?:visual|layout|reference)|visual[^\n]*mobile/i);
  assert.match(visual, /manual-device|interactive[^\n]*device/i);
  assert.match(visual, /production[^\n]*(?:not|remain|protected)/i);
  assert.doesNotMatch(visual, /bootstrap shell/i);
  assert.doesNotMatch(visual, /ERR_BLOCKED_BY_ADMINISTRATOR/);
});
