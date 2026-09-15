import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workflow = fs.readFileSync('.github/workflows/gitlab-variable-bootstrap.yml', 'utf8');
const protectionWorkflow = fs.readFileSync('.github/workflows/gitlab-protection-bootstrap.yml', 'utf8');
const protectionScript = fs.readFileSync('scripts/ci/bootstrap-gitlab-protected-tags.sh', 'utf8');

test('GitLab variable bootstrap reuses the project-scoped migration token and never prints secret values', () => {
  assert.match(workflow, /GITLAB_MIGRATION_TOKEN: \$\{\{ secrets\.GITLAB_MIGRATION_TOKEN \}\}/);
  assert.doesNotMatch(workflow, /GITLAB_SETUP_TOKEN/);
  assert.doesNotMatch(workflow, /set -x|echo \"\$\{?(BL_KEYSTORE_BASE64|BL_KEYSTORE_PASSWORD|BL_KEY_ALIAS|FIREBASE_GOOGLE_SERVICES_JSON)/);
});

test('GitLab variable bootstrap copies all required protected variables from existing GitHub secrets', () => {
  for (const key of ['BL_KEYSTORE_BASE64', 'BL_KEYSTORE_PASSWORD', 'BL_KEY_ALIAS', 'FIREBASE_GOOGLE_SERVICES_JSON']) {
    assert.match(workflow, new RegExp(`${key}: \\$\\{\\{ secrets\\.${key} \\}\\}`));
  }
  assert.match(workflow, /protected=true/);
  assert.match(workflow, /masked=true/);
});

test('Firebase JSON is compacted before masked storage so GitLab receives a single-line value', () => {
  assert.match(workflow, /firebase_compact=/);
  assert.match(workflow, /json\.loads\(os\.environ\['FIREBASE_GOOGLE_SERVICES_JSON'\]\)/);
  assert.match(workflow, /separators=\(',', ':'\)/);
  assert.match(workflow, /upsert_variable FIREBASE_GOOGLE_SERVICES_JSON \"\$firebase_compact\"/);
  assert.doesNotMatch(workflow, /FIREBASE_GOOGLE_SERVICES_JSON .* file false/);
});

test('GitLab variable bootstrap fails closed when the migration token lacks variable write access', () => {
  assert.match(workflow, /Variable write access is not available on GITLAB_MIGRATION_TOKEN/);
  assert.match(workflow, /exit 1/);
});

test('GitLab protection bootstrap reuses only the project-scoped migration token', () => {
  assert.match(protectionWorkflow, /GITLAB_MIGRATION_TOKEN: \$\{\{ secrets\.GITLAB_MIGRATION_TOKEN \}\}/);
  assert.doesNotMatch(protectionWorkflow, /GITLAB_SETUP_TOKEN|BL_KEYSTORE|FIREBASE_GOOGLE_SERVICES_JSON/);
  assert.match(protectionWorkflow, /bash scripts\/ci\/bootstrap-gitlab-protected-tags\.sh/);
});

test('GitLab protection bootstrap additively protects every release tag family without weakening existing rules', () => {
  for (const pattern of ['v*', 'admin-v*', 'nova-v*']) {
    assert.match(protectionScript, new RegExp(pattern.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  }
  assert.match(protectionScript, /\/protected_tags/);
  assert.match(protectionScript, /create_access_level=40/);
  assert.match(protectionScript, /Protected tag rule already exists/);
  assert.match(protectionScript, /Created protected tag rule/);
  assert.doesNotMatch(protectionScript, /--request DELETE|--request PUT[^\n]*protected_tags|--request PATCH[^\n]*protected_tags/);
});

test('GitLab protection bootstrap never mutates protected branches or repository refs', () => {
  assert.doesNotMatch(protectionScript, /protected_branches|git push|merge_requests/);
});
