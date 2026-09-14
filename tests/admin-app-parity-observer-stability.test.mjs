import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const parity = readFileSync(new URL('../admin/admin-app-parity.js', import.meta.url), 'utf8');
const workspace = readFileSync(new URL('../admin/workspace.html', import.meta.url), 'utf8');

test('Admin parity observer cannot retrigger itself through unchanged DOM writes', () => {
  assert.match(parity, /function setTextIfChanged\(/);
  assert.match(parity, /if\(node\.textContent!==next\)node\.textContent=next/);
  assert.match(parity, /function setStateIfChanged\(/);
  assert.match(parity, /let observerScheduled=false/);
  assert.match(parity, /if\(observerScheduled\)return/);
  assert.match(parity, /requestAnimationFrame\(\(\)=>\{observerScheduled=false;syncHealth\(\);updateLiveStatus\(\)\}\)/);
  assert.doesNotMatch(parity, /target\.textContent=source\.textContent\|\|'—'/);
  assert.equal(workspace.match(/admin-app-parity\.js\?v=5/g)?.length, 2);
});
