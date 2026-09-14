import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workflow = fs.readFileSync('.github/workflows/gitlab-variable-bootstrap.yml', 'utf8');

test('GitLab variable bootstrap uses a dedicated setup token and never prints secret values', () => {
  assert.match(workflow, /GITLAB_SETUP_TOKEN: \$\{\{ secrets\.GITLAB_SETUP_TOKEN \}\}/);
  assert.doesNotMatch(workflow, /set -x|echo \"\$\{?(BL_KEYSTORE_BASE64|BL_KEYSTORE_PASSWORD|BL_KEY_ALIAS|FIREBASE_GOOGLE_SERVICES_JSON)/);
});

test('GitLab variable bootstrap copies all required protected variables from existing GitHub secrets', () => {
  for (const key of ['BL_KEYSTORE_BASE64', 'BL_KEYSTORE_PASSWORD', 'BL_KEY_ALIAS', 'FIREBASE_GOOGLE_SERVICES_JSON']) {
    assert.match(workflow, new RegExp(`${key}: \\$\\{\\{ secrets\\.${key} \\}\\}`));
  }
  assert.match(workflow, /protected=true/);
  assert.match(workflow, /masked=true/);
});

test('GitLab variable bootstrap only creates or updates CI variable metadata', () => {
  assert.match(workflow, /\/variables\/\$encoded_key/);
  assert.match(workflow, /--request POST/);
  assert.match(workflow, /--request PUT/);
  assert.doesNotMatch(workflow, /protected_tags|protected_branches|git push|merge_requests/);
});
