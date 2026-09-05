import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/guardian-status.js', 'utf8');

assert.match(
  source,
  /const blocked=evidence\.filter\(x=>x\.run\?\.status==='completed'&&x\.run\.conclusion!=='success'\)/,
  'every completed Guardian evidence gate that did not succeed must block validation'
);
assert.match(
  source,
  /const healthy=evidence\.filter\(x=>x\.run\?\.status==='completed'&&x\.run\.conclusion==='success'\)/,
  'only successful Guardian evidence gates may count as validated'
);
assert.doesNotMatch(
  source,
  /conclusion==='success'\|\|x\.run\.conclusion==='skipped'/,
  'skipped Guardian evidence must not count as validated'
);
assert.match(source, /s==='RUNNING'\?'':'high'/, 'skipped or otherwise unsuccessful evidence must use blocking tone');
assert.match(source, /blocked\.length\?'BLOCKED':running\.length\?'INCOMPLETE'/, 'Guardian state must fail closed for unsuccessful evidence');

console.log('nova guardian required-gate evidence regression: PASS');
