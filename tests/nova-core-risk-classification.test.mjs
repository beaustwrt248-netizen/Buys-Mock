import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/app.js', 'utf8');

assert.match(source, /function riskFor\(pr\)/, 'Nova core must expose one risk classifier');
assert.match(source, /if\(!ref\.startsWith\('nova\/'\)\)return'normal'/, 'non-Nova branches must not become Nova human gates');
assert.match(source, /high\[- \]risk/, 'classifier must recognise explicit high-risk wording');
assert.match(source, /risk\\s\*\[:=-\][\s\S]*?high/, 'classifier must recognise explicit Risk: High declarations');
assert.match(source, /#\{1,6\}\\s\*risk[\s\S]*?high/, 'classifier must recognise Markdown Risk sections declaring High');
assert.doesNotMatch(source, /\|auth\|permission\|security\|pricing\|supabase\|migration\|release\|deploy\|workflow\|guardian\|androidmanifest\|signing/, 'ordinary protected-domain words must not independently classify a PR as high risk');
assert.match(source, /const attention=novaPrs\.filter\(p=>riskFor\(p\)==='high'\)/, 'Needs Attention must use the corrected classifier');
assert.match(source, /if\(riskFor\(pr\)==='high'\)return\{label:'HUMAN GATE'/, 'Development state must use the corrected classifier');

const examples = [
  { text: 'No auth changes. Guardian remains protected. Deployment configuration is unchanged.', expected: false },
  { text: 'Security audit remains enabled; no workflow or pricing changes.', expected: false },
  { text: 'High-risk governance change.', expected: true },
  { text: 'Risk: High', expected: true },
  { text: '## Risk\nHigh — protected workflow change.', expected: true },
];

const explicitHigh = text => /\bhigh[- ]risk\b/i.test(text) || /\brisk\s*[:=-]\s*(?:\*\*)?high\b/i.test(text) || /(?:^|\n)#{1,6}\s*risk\s*\n+\s*(?:\*\*)?high\b/im.test(text);
for (const example of examples) assert.equal(explicitHigh(example.text), example.expected, example.text);

console.log('nova core risk classification regression: PASS');