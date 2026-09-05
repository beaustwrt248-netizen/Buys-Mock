import fs from 'node:fs';
import assert from 'node:assert/strict';

const files = [
  ['release readiness', fs.readFileSync('nova/release-readiness.js', 'utf8')],
  ['Guardian status', fs.readFileSync('nova/guardian-status.js', 'utf8')],
  ['Recommendations', fs.readFileSync('nova/recommendations.js', 'utf8')],
];

for (const [label, source] of files) {
  assert.match(source, /let refreshVersion=0;/, `${label} must track refresh freshness`);
  assert.match(source, /const requestVersion=\+\+refreshVersion;/, `${label} must version each refresh before async work`);
  assert.match(source, /if\(requestVersion!==refreshVersion\)return;/, `${label} must reject stale successful responses`);
  assert.match(source, /catch\(error\)\{if\(requestVersion!==refreshVersion\)return;/, `${label} must reject stale failures before UI mutation`);
}

console.log('nova panel refresh freshness contract: PASS');
