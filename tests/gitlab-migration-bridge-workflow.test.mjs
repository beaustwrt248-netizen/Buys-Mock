import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workflow = fs.readFileSync('.github/workflows/gitlab-migration-bridge.yml', 'utf8');

test('migration bridge cancels stale observations when a newer migration commit arrives', () => {
  assert.match(workflow, /cancel-in-progress:\s*true/);
});

test('migration bridge fails closed when GitLab main differs from GitHub main', () => {
  assert.match(
    workflow,
    /if \[\[ "\$github_main" != "\$gitlab_main" \]\]; then[\s\S]*?GitLab main does not currently match GitHub main[\s\S]*?exit 1[\s\S]*?fi/,
  );
});

test('migration bridge detects repeatedly pending tagged GitLab jobs and records evidence', () => {
  assert.match(workflow, /detect-gitlab-runner-block\.mjs/);
  assert.match(workflow, /PENDING_TAGGED_THRESHOLD=8/);
  assert.match(workflow, /blocked_runner/);
  assert.match(workflow, /tag_list/);
});
