import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/app.js', 'utf8');

assert.match(source, /function riskFor\(pr\)[\s\S]*?high\[- \]risk/, 'overview risk classifier must recognise explicit high-risk wording');
assert.match(source, /risk\\s\*\[:=-\][\s\S]*?high/, 'overview risk classifier must recognise Risk: High declarations');
assert.match(source, /#\{1,6\}\\s\*risk[\s\S]*?high/, 'overview risk classifier must recognise Markdown Risk sections declaring High');
assert.match(source, /if\(!ref\.startsWith\('nova\/'\)\)return'normal'/, 'non-Nova branches must not be classified as Nova human gates');
assert.doesNotMatch(source, /risk\\s\*\[:\\-\]\?\\s\*high\|auth\|permission\|security\|pricing/, 'overview must not use broad domain-keyword high-risk inference');

assert.match(source, /const healthy=current\.filter\(r=>r\.status==='completed'&&r\.conclusion==='success'\)/, 'only successful workflow runs may count as healthy');
assert.match(source, /const skipped=current\.filter\(r=>r\.status==='completed'&&r\.conclusion==='skipped'\)/, 'skipped workflow evidence must be tracked separately');
assert.match(source, /skipped\.length\?'INCOMPLETE':'HEALTHY'/, 'monitoring must not report HEALTHY when current evidence is skipped');
assert.match(source, /const skippedRuns=currentRuns\.filter/, 'overview core must track skipped current workflow evidence');
assert.match(source, /else if\(skippedRuns\.length\)\{\$\('coreState'\)\.textContent='INCOMPLETE'/, 'overview core must not report HEALTHY when current workflow evidence is skipped');

console.log('nova overview classification contract: PASS');
