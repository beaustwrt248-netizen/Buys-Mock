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

test('migration bridge reports both non-secret main SHAs before refusing drifted refs', () => {
  assert.match(workflow, /echo "GitHub main: \$github_main"/);
  assert.match(workflow, /echo "GitLab main: \$gitlab_main"/);
});

test('migration bridge creates only missing tags and never force-overwrites an existing GitLab tag', () => {
  assert.match(workflow, /Sync missing tags without overwriting existing tags/);
  assert.match(workflow, /git push gitlab "refs\/tags\/\$tag:refs\/tags\/\$tag"/);
  assert.match(workflow, /Refusing conflicting GitLab tag/);
  assert.doesNotMatch(workflow, /git push[^\n]*--force[^\n]*refs\/tags/);
});

test('migration bridge snapshots GitLab tags once instead of doing one network lookup per local tag', () => {
  assert.match(workflow, /git ls-remote --tags gitlab > \/tmp\/gitlab-tags-before\.txt/);
  assert.match(workflow, /declare -A remote_tags=\(\)/);
  const syncBlock = workflow.match(/- name: Sync missing tags without overwriting existing tags[\s\S]*?- name: Verify tag coverage after create-only synchronization/)?.[0] ?? '';
  assert.equal((syncBlock.match(/git ls-remote --tags gitlab/g) ?? []).length, 1);
  assert.doesNotMatch(syncBlock, /git ls-remote gitlab "refs\/tags\/\$tag"/);
});

test('migration bridge verifies full tag parity after create-only synchronization', () => {
  assert.match(workflow, /Verify tag coverage after create-only synchronization/);
  assert.match(workflow, /GitLab tag refs match GitHub/);
});

test('migration bridge detects repeatedly pending tagged GitLab jobs and records evidence', () => {
  assert.match(workflow, /detect-gitlab-runner-block\.mjs/);
  assert.match(workflow, /PENDING_TAGGED_THRESHOLD=8/);
  assert.match(workflow, /blocked_runner/);
  assert.match(workflow, /tag_list/);
});

test('blocked-runner diagnostics use a safe JSON handoff instead of a fragile Python f-string one-liner', () => {
  assert.match(workflow, /DETECTOR_JSON="\$detector" python3 - <<'PY'/);
  assert.match(workflow, /json\.loads\(os\.environ\['DETECTOR_JSON'\]\)/);
  assert.doesNotMatch(workflow, /python3 -c 'import json,sys; d=json\.load\(sys\.stdin\); \[print\(f/);
});
