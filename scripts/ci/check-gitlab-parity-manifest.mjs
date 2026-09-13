import fs from 'node:fs';
import path from 'node:path';

const manifestPath = 'ci/gitlab-parity-manifest.json';
const workflowDir = '.github/workflows';
const allowedCategories = new Set([
  'android',
  'admin',
  'ota-release',
  'guardian-security',
  'governance-contract',
  'quality',
]);

if (!fs.existsSync(manifestPath)) {
  console.error(`Missing ${manifestPath}`);
  process.exit(1);
}
if (!fs.existsSync(workflowDir)) {
  console.error(`Missing ${workflowDir}`);
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
if (!Array.isArray(manifest.workflows) || manifest.workflows.length === 0) {
  console.error('Manifest must contain at least one workflow');
  process.exit(1);
}

const seenIds = new Set();
const seenWorkflows = new Set();
const seenJobs = new Set();
for (const item of manifest.workflows) {
  for (const key of ['id', 'githubWorkflow', 'category', 'gitlabJob']) {
    if (typeof item[key] !== 'string' || item[key].trim() === '') {
      console.error(`Manifest entry ${item.id ?? '<unknown>'} is missing ${key}`);
      process.exit(1);
    }
  }
  if (!allowedCategories.has(item.category)) {
    console.error(`Manifest entry ${item.id} has unsupported category: ${item.category}`);
    process.exit(1);
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
  if (seenJobs.has(item.gitlabJob)) {
    console.error(`Duplicate GitLab job mapping: ${item.gitlabJob}`);
    process.exit(1);
  }
  seenIds.add(item.id);
  seenWorkflows.add(item.githubWorkflow);
  seenJobs.add(item.gitlabJob);
}

const diskWorkflows = fs.readdirSync(workflowDir)
  .filter((name) => /\.ya?ml$/i.test(name))
  .sort();
const mappedWorkflows = [...seenWorkflows].sort();
const missingMappings = diskWorkflows.filter((name) => !seenWorkflows.has(name));
const staleMappings = mappedWorkflows.filter((name) => !fs.existsSync(path.join(workflowDir, name)));
if (missingMappings.length || staleMappings.length) {
  if (missingMappings.length) console.error(`Unmapped GitHub workflows: ${missingMappings.join(', ')}`);
  if (staleMappings.length) console.error(`Stale manifest workflow mappings: ${staleMappings.join(', ')}`);
  process.exit(1);
}

const gitlabText = ['.gitlab-ci.yml', ...fs.readdirSync('.gitlab/ci').filter((name) => /\.ya?ml$/i.test(name)).map((name) => `.gitlab/ci/${name}`)]
  .map((file) => fs.readFileSync(file, 'utf8'))
  .join('\n');
for (const item of manifest.workflows.filter((entry) => entry.required === true)) {
  const escaped = item.gitlabJob.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  if (!new RegExp(`^${escaped}:\\s*$`, 'm').test(gitlabText)) {
    console.error(`Required GitLab job is not defined: ${item.gitlabJob}`);
    process.exit(1);
  }
}

console.log(`Validated complete parity mapping for ${diskWorkflows.length} GitHub workflows and ${manifest.workflows.length} GitLab mappings.`);
