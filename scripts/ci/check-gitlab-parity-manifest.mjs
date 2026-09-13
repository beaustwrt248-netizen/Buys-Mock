import fs from 'node:fs';

const path = 'ci/gitlab-parity-manifest.json';
if (!fs.existsSync(path)) {
  console.error(`Missing ${path}`);
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(path, 'utf8'));
if (!Array.isArray(manifest.workflows) || manifest.workflows.length === 0) {
  console.error('Manifest must contain at least one workflow');
  process.exit(1);
}

const seenIds = new Set();
const seenWorkflows = new Set();
for (const item of manifest.workflows) {
  for (const key of ['id', 'githubWorkflow', 'category', 'gitlabJob']) {
    if (!item[key]) {
      console.error(`Manifest entry ${item.id ?? '<unknown>'} is missing ${key}`);
      process.exit(1);
    }
  }
  if (item.required !== true && item.required !== false) {
    console.error(`Manifest entry ${item.id} must set required to true or false`);
    process.exit(1);
  }
  if (seenIds.has(item.id)) {
    console.error(`Duplicate manifest id: ${item.id}`);
    process.exit(1);
  }
  if (seenWorkflows.has(item.githubWorkflow)) {
    console.error(`Duplicate GitHub workflow mapping: ${item.githubWorkflow}`);
    process.exit(1);
  }
  seenIds.add(item.id);
  seenWorkflows.add(item.githubWorkflow);
}

console.log(`Validated ${manifest.workflows.length} GitHub workflow mappings for GitLab parity.`);
