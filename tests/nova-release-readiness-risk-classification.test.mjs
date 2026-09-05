import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/release-readiness.js', 'utf8');

assert.match(source, /function isHighRiskNovaPr\(pr\)/, 'release readiness must use an explicit Nova risk classifier');
assert.match(source, /if\(!ref\.startsWith\('nova\/'\)\)return false/, 'only Nova branches belong in Nova governance readiness');
assert.match(source, /\\bhigh\[- \]risk\\b/, 'explicit high-risk wording must be recognised');
assert.match(source, /\\brisk\\s\*\[:=-\]/, 'explicit Risk: High declarations must be recognised');
assert.match(source, /#{1,6}\\s\*risk/, 'markdown Risk headings followed by High must be recognised');
assert.match(source, /\.filter\(isHighRiskNovaPr\)/, 'open PR readiness must use the explicit risk classifier');

assert.doesNotMatch(
  source,
  /risk\\s\*\[:\\-\]\?\\s\*high\|auth\|permission\|security\|pricing\|supabase\|migration\|release\|deploy\|workflow\|guardian\|androidmanifest\|signing/,
  'ordinary safety prose must not classify a Nova PR as high risk merely because it mentions security, release or workflow terms'
);

console.log('nova release readiness risk classification regression: PASS');
