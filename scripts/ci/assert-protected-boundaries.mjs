import fs from 'node:fs';

const manifest = JSON.parse(fs.readFileSync('ci/gitlab-parity-manifest.json', 'utf8'));
const guardian = manifest.workflows.filter((item) => item.category === 'guardian-security');
if (guardian.length === 0) {
  console.error('No guardian-security workflow mappings found');
  process.exit(1);
}
for (const item of guardian) {
  if (item.required !== true || !item.gitlabJob.startsWith('security:')) {
    console.error(`Unsafe Guardian mapping: ${item.id}`);
    process.exit(1);
  }
}

const security = fs.readFileSync('.gitlab/ci/security.yml', 'utf8');
const admin = fs.readFileSync('.gitlab/ci/admin.yml', 'utf8');
const release = fs.readFileSync('.gitlab/ci/release.yml', 'utf8');
const root = fs.readFileSync('.gitlab-ci.yml', 'utf8');
const requiredText = [
  [security, 'final merge remains human-controlled', 'Guardian human merge boundary'],
  [security, 'authority_expansion:false', 'Guardian authority-expansion boundary'],
  [security, 'raw_evidence_exposed:false', 'Guardian raw-evidence boundary'],
  [admin, 'when: manual', 'Admin recovery manual gate'],
  [admin, 'BL_KEYSTORE_BASE64', 'Admin signing secret guard'],
  [release, '.pending-authenticated-release:', 'Authenticated release blocker'],
  [release, "when: manual", 'Release manual gate'],
  [root, 'security.yml', 'Security pipeline include'],
];
for (const [text, needle, label] of requiredText) {
  if (!text.includes(needle)) {
    console.error(`Missing protected boundary: ${label}`);
    process.exit(1);
  }
}

const privilegedFiles = `${admin}\n${release}`;
if (/echo\s+['"]?\$\{?(?:BL_KEYSTORE|BL_KEY_ALIAS|SUPABASE_SERVICE|GITLAB_TOKEN)/i.test(privilegedFiles)) {
  console.error('A privileged GitLab job appears to echo a protected variable');
  process.exit(1);
}

console.log(`Validated ${guardian.length} protected Guardian/security mappings plus manual release/signing boundaries.`);
