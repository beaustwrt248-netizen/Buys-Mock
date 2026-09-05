import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/release-readiness.js', 'utf8');

assert.match(
  source,
  /const blocked=checks\.filter\(x=>x\.run\?\.status==='completed'&&x\.run\.conclusion!=='success'\)/,
  'every completed required gate that did not succeed, including skipped/cancelled/timed-out runs, must block readiness'
);
assert.doesNotMatch(
  source,
  /x\.run\.conclusion!=='success'&&x\.run\.conclusion!=='skipped'/,
  'skipped required gates must never be treated as successful release evidence'
);
assert.match(source, /const ready=blocked\.length===0&&running\.length===0&&deployHealthy&&highRisk\.length===0/, 'READY must require zero unsuccessful and zero incomplete required gates');
assert.match(source, /s==='RUNNING'\?'':'high'/, 'a skipped required gate must render with blocking tone rather than neutral tone');
assert.match(source, /required current check.*not complete successfully/, 'blocked readiness should explain that required evidence did not succeed');

console.log('nova release readiness required-gates regression: PASS');
