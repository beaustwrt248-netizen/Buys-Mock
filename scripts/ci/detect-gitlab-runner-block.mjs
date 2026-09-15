import fs from 'node:fs';

const previousPolls = Number.parseInt(process.env.PENDING_TAGGED_POLLS ?? '0', 10);
const threshold = Number.parseInt(process.env.PENDING_TAGGED_THRESHOLD ?? '8', 10);

if (!Number.isInteger(previousPolls) || previousPolls < 0) {
  console.error('PENDING_TAGGED_POLLS must be a non-negative integer');
  process.exit(1);
}
if (!Number.isInteger(threshold) || threshold < 1) {
  console.error('PENDING_TAGGED_THRESHOLD must be a positive integer');
  process.exit(1);
}

let jobs;
try {
  jobs = JSON.parse(fs.readFileSync(0, 'utf8'));
} catch (error) {
  console.error(`Unable to parse GitLab jobs JSON: ${error.message}`);
  process.exit(1);
}

if (!Array.isArray(jobs)) {
  console.error('GitLab jobs payload must be an array');
  process.exit(1);
}

const blockedJobs = jobs
  .filter((job) => job?.status === 'pending' && Array.isArray(job?.tag_list) && job.tag_list.length > 0)
  .map((job) => ({
    name: String(job.name ?? ''),
    tags: job.tag_list.map((tag) => String(tag)),
  }))
  .filter((job) => job.name.length > 0)
  .sort((a, b) => a.name.localeCompare(b.name));

const nextPendingTaggedPolls = blockedJobs.length > 0 ? previousPolls + 1 : 0;
const output = {
  blocked: blockedJobs.length > 0 && nextPendingTaggedPolls >= threshold,
  blockedJobs,
  nextPendingTaggedPolls,
  threshold,
};

process.stdout.write(`${JSON.stringify(output)}\n`);
