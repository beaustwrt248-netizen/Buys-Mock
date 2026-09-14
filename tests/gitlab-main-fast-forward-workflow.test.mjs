import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workflowPath = '.github/workflows/gitlab-main-fast-forward.yml';

test('GitLab main sync is non-force and only fast-forwards an ancestor', () => {
  const workflow = fs.readFileSync(workflowPath, 'utf8');
  assert.match(workflow, /merge-base --is-ancestor/);
  assert.match(workflow, /git push gitlab "\$github_main:refs\/heads\/main"/);
  assert.doesNotMatch(workflow, /--force|\+[^\n]*refs\/heads\/main/);
});

test('GitLab main sync verifies both the precondition and resulting SHA', () => {
  const workflow = fs.readFileSync(workflowPath, 'utf8');
  assert.match(workflow, /gitlab_main_before/);
  assert.match(workflow, /gitlab_main_after/);
  assert.match(workflow, /GitLab main changed during verification/);
  assert.match(workflow, /GitLab main did not reach the expected GitHub main SHA/);
});

test('GitLab main sync never writes GitHub origin', () => {
  const workflow = fs.readFileSync(workflowPath, 'utf8');
  assert.doesNotMatch(workflow, /git push origin/);
});
