import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

const script = 'scripts/ci/detect-gitlab-runner-block.mjs';

function detect(jobs, pendingPolls) {
  return spawnSync(process.execPath, [script], {
    cwd: process.cwd(),
    env: { ...process.env, PENDING_TAGGED_POLLS: String(pendingPolls), PENDING_TAGGED_THRESHOLD: '3' },
    input: JSON.stringify(jobs),
    encoding: 'utf8',
  });
}

test('fails closed after repeated pending tagged jobs', () => {
  const result = detect([
    { name: 'admin:android-check', status: 'pending', tag_list: ['morley-android'] },
    { name: 'test:quality-gate', status: 'success', tag_list: [] },
  ], 2);

  assert.equal(result.status, 0, result.stderr);
  const output = JSON.parse(result.stdout);
  assert.equal(output.nextPendingTaggedPolls, 3);
  assert.equal(output.blocked, true);
  assert.deepEqual(output.blockedJobs, [
    { name: 'admin:android-check', tags: ['morley-android'] },
  ]);
});

test('does not declare a runner block before the threshold', () => {
  const result = detect([
    { name: 'test:nova-next-ci', status: 'pending', tag_list: ['morley-android'] },
  ], 1);

  assert.equal(result.status, 0, result.stderr);
  const output = JSON.parse(result.stdout);
  assert.equal(output.nextPendingTaggedPolls, 2);
  assert.equal(output.blocked, false);
});

test('resets the pending counter when tagged jobs are no longer pending', () => {
  const result = detect([
    { name: 'admin:android-check', status: 'running', tag_list: ['morley-android'] },
  ], 2);

  assert.equal(result.status, 0, result.stderr);
  const output = JSON.parse(result.stdout);
  assert.equal(output.nextPendingTaggedPolls, 0);
  assert.equal(output.blocked, false);
  assert.deepEqual(output.blockedJobs, []);
});
