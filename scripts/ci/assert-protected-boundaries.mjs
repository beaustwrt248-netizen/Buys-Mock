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
console.log(`Validated ${guardian.length} protected Guardian/security mappings.`);
