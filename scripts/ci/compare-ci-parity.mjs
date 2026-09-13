import fs from 'node:fs';

const [githubPath, gitlabPath] = process.argv.slice(2);
if (!githubPath || !gitlabPath) {
  console.error('Usage: node scripts/ci/compare-ci-parity.mjs <github.json> <gitlab.json>');
  process.exit(1);
}

const manifest = JSON.parse(fs.readFileSync('ci/gitlab-parity-manifest.json', 'utf8'));
const github = JSON.parse(fs.readFileSync(githubPath, 'utf8'));
const gitlab = JSON.parse(fs.readFileSync(gitlabPath, 'utf8'));

if (!github.sha || !gitlab.sha || github.sha !== gitlab.sha) {
  console.error(`Provider SHA mismatch: github=${github.sha ?? '<missing>'} gitlab=${gitlab.sha ?? '<missing>'}`);
  process.exit(1);
}

for (const provider of [github, gitlab]) {
  if (!Array.isArray(provider.jobs)) {
    console.error('Each provider input must contain a jobs array');
    process.exit(1);
  }
}

const byName = (provider) => new Map(provider.jobs.map((job) => [job.name, job]));
const githubJobs = byName(github);
const gitlabJobs = byName(gitlab);
let failed = false;

for (const item of manifest.workflows.filter((entry) => entry.required === true)) {
  const gh = githubJobs.get(item.githubWorkflow) ?? githubJobs.get(item.id);
  const gl = gitlabJobs.get(item.gitlabJob);

  if (!gh) {
    console.error(`Missing GitHub result for required workflow: ${item.githubWorkflow}`);
    failed = true;
    continue;
  }
  if (!gl) {
    console.error(`Missing GitLab result for required job: ${item.gitlabJob}`);
    failed = true;
    continue;
  }
  if (gh.status !== 'success') {
    console.error(`GitHub required workflow did not succeed: ${item.githubWorkflow} (${gh.status})`);
    failed = true;
  }
  if (gl.status !== 'success') {
    console.error(`GitLab required job did not succeed: ${item.gitlabJob} (${gl.status})`);
    failed = true;
  }

  const ghArtifacts = [...(gh.artifacts ?? [])].sort();
  const glArtifacts = [...(gl.artifacts ?? [])].sort();
  if (ghArtifacts.length > 0) {
    const missing = ghArtifacts.filter((name) => !glArtifacts.includes(name));
    if (missing.length > 0) {
      console.error(`GitLab artifacts missing for ${item.id}: ${missing.join(', ')}`);
      failed = true;
    }
  }
}

if (failed) process.exit(1);
console.log(`CI parity PASS for ${github.sha}`);
