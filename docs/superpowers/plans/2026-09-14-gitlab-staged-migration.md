# GitLab Staged Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move `beaustwrt248-netizen/Buys-Mock` to GitLab-primary development with CI/build/security parity while keeping GitHub intact as the rollback source until cutover is proven safe.

**Architecture:** Use a staged dual-platform migration. First inventory the existing GitHub Actions responsibilities and capture a machine-readable parity manifest, then add GitLab CI orchestration that calls the same repository commands wherever possible. Run GitHub Actions and GitLab CI against the same revision, compare results and artifacts, and only then switch development/deployment integrations to GitLab while retaining GitHub as a mirror.

**Tech Stack:** Git, GitHub Actions, GitLab CI/CD, Android/Gradle, repository test scripts, Guardian/security checks, Vercel, Supabase.

**Spec:** `docs/superpowers/specs/2026-09-14-gitlab-staged-migration-design.md`

## Global Constraints

- Preserve all Git history, branches, and tags.
- Keep `main` and production behavior unchanged during migration validation.
- Recreate critical GitHub Actions behavior in GitLab CI rather than redesigning product functionality.
- Verify Android/APK, Admin, OTA, Guardian/security, governance, release, and general quality checks before cutover.
- Keep GitHub available as a rollback source during validation.
- Do not change Vercel or Supabase production integration until repository and CI parity is demonstrated.
- Never force-push or rewrite `main` as part of migration.
- Never remove GitHub workflows before their GitLab equivalents are validated.
- Never commit signing credentials, deployment tokens, or other secrets.
- Preserve Guardian human-approval and protected-repair boundaries.

---

### Task 1: Capture the GitHub CI parity baseline

**Files:**
- Create: `docs/migration/gitlab-workflow-parity.md`
- Create: `scripts/ci/check-gitlab-parity-manifest.mjs`
- Create: `ci/gitlab-parity-manifest.json`

**Interfaces:**
- Consumes: `.github/workflows/*.yml`, repository scripts referenced by those workflows.
- Produces: `ci/gitlab-parity-manifest.json` with entries shaped as `{ "id": string, "githubWorkflow": string, "category": string, "required": boolean, "gitlabJob": string }` and a validator that exits non-zero if required fields or mappings are missing.

- [ ] **Step 1: Write the failing manifest validator**

Create `scripts/ci/check-gitlab-parity-manifest.mjs` with validation for a missing/empty manifest and for required entries without `gitlabJob`:

```js
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
}
```

- [ ] **Step 2: Run the validator to verify it fails before the manifest exists**

Run:

```bash
node scripts/ci/check-gitlab-parity-manifest.mjs
```

Expected: non-zero exit with `Missing ci/gitlab-parity-manifest.json`.

- [ ] **Step 3: Inventory every existing GitHub workflow and create the manifest**

Enumerate `.github/workflows/*.yml`, read the commands each workflow executes, and add one manifest entry per workflow. Categorize each entry into exactly one of:

```text
android
admin
ota-release
guardian-security
governance-contract
quality
```

The manifest root must be:

```json
{
  "version": 1,
  "workflows": []
}
```

Known required workflows already visible in the repository include `admin-android-check.yml`, `admin-apk-build.yml`, `admin-control-integration.yml`, `admin-device-governance.yml`, `admin-ota-feed-deploy.yml`, `admin-recovery-apk.yml`, `admin-release-check.yml`, `admin-support-audit-check.yml`, `admin-support-governance.yml`, `admin-support-reconcile-check.yml`, `android-pr-permissions-guard.yml`, `android-pr-readonly.yml`, plus every additional workflow present in `.github/workflows/` at execution time.

Document each workflow's trigger, commands, artifacts, secrets/permissions, and intended GitLab job in `docs/migration/gitlab-workflow-parity.md`.

- [ ] **Step 4: Run the validator and verify the baseline is complete**

Run:

```bash
node scripts/ci/check-gitlab-parity-manifest.mjs
```

Expected: exit 0.

- [ ] **Step 5: Commit the baseline**

```bash
git add ci/gitlab-parity-manifest.json scripts/ci/check-gitlab-parity-manifest.mjs docs/migration/gitlab-workflow-parity.md
git commit -m "ci: capture GitHub workflow parity baseline"
```

---

### Task 2: Add the GitLab CI orchestration skeleton without changing product behavior

**Files:**
- Create: `.gitlab-ci.yml`
- Create: `.gitlab/ci/validate.yml`
- Create: `.gitlab/ci/security.yml`
- Create: `.gitlab/ci/android.yml`
- Create: `.gitlab/ci/admin.yml`
- Create: `.gitlab/ci/release.yml`

**Interfaces:**
- Consumes: repository commands identified in Task 1.
- Produces: GitLab stages `validate`, `test`, `security`, `build`, `release-check` and jobs whose names match the `gitlabJob` values in the parity manifest.

- [ ] **Step 1: Add the root pipeline file**

Create `.gitlab-ci.yml`:

```yaml
stages:
  - validate
  - test
  - security
  - build
  - release-check

include:
  - local: .gitlab/ci/validate.yml
  - local: .gitlab/ci/security.yml
  - local: .gitlab/ci/android.yml
  - local: .gitlab/ci/admin.yml
  - local: .gitlab/ci/release.yml

workflow:
  rules:
    - if: '$CI_PIPELINE_SOURCE == "merge_request_event"'
    - if: '$CI_COMMIT_BRANCH'
    - if: '$CI_COMMIT_TAG'
```

- [ ] **Step 2: Add a validation job that proves the parity manifest is enforced**

Create `.gitlab/ci/validate.yml`:

```yaml
validate:parity-manifest:
  stage: validate
  image: node:22-bookworm
  script:
    - node scripts/ci/check-gitlab-parity-manifest.mjs
```

- [ ] **Step 3: Add one GitLab job per required parity-manifest entry**

For every required manifest item, define a job in the matching fragment. Each job must execute the same repository command(s) captured from its GitHub workflow instead of re-implementing those checks in YAML. Use `rules:changes` only when the GitHub workflow already scopes execution equivalently; otherwise preserve the broader behavior.

Jobs that emit APK/AAB files or reports must use GitLab `artifacts:` with the same effective outputs retained for at least 7 days during migration validation.

- [ ] **Step 4: Ensure secret-dependent jobs fail closed when required variables are absent**

Use explicit guards such as:

```yaml
before_script:
  - test -n "$ANDROID_KEYSTORE_BASE64" || { echo "ANDROID_KEYSTORE_BASE64 is required"; exit 1; }
```

Do not print variable values. Mark signing/deployment variables as masked and protected in GitLab project settings during Task 5.

- [ ] **Step 5: Validate YAML and commit**

Run a local YAML parse/lint command already available in the repository, or if none exists:

```bash
ruby -e "require 'yaml'; YAML.load_file('.gitlab-ci.yml'); Dir['.gitlab/ci/*.yml'].each { |f| YAML.load_file(f) }"
node scripts/ci/check-gitlab-parity-manifest.mjs
```

Expected: both commands exit 0.

Commit:

```bash
git add .gitlab-ci.yml .gitlab/ci ci/gitlab-parity-manifest.json
git commit -m "ci: add GitLab pipeline parity jobs"
```

---

### Task 3: Preserve Guardian/security and permission boundaries explicitly

**Files:**
- Modify: `.gitlab/ci/security.yml`
- Modify: `ci/gitlab-parity-manifest.json`
- Create: `scripts/ci/assert-protected-boundaries.mjs`

**Interfaces:**
- Consumes: Guardian/security workflow behavior and protected repair boundaries documented by existing GitHub workflows.
- Produces: a CI check that fails if a protected GitLab job is configured to run with unsafe branch/tag scope or if a required Guardian mapping is absent.

- [ ] **Step 1: Write the boundary assertion script**

Create `scripts/ci/assert-protected-boundaries.mjs` that verifies every `guardian-security` manifest entry is `required: true` and has a mapped job name beginning with `security:`.

```js
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
```

- [ ] **Step 2: Run it before the manifest is corrected**

```bash
node scripts/ci/assert-protected-boundaries.mjs
```

Expected: fail if any Guardian/security mapping is absent or not named under `security:`.

- [ ] **Step 3: Correct the manifest and security jobs**

Map all Guardian/security and permission workflows to `security:*` GitLab jobs. Keep human-approval/deployment actions manual where the GitHub workflow requires approval or protected execution. Use GitLab `when: manual` and protected branches/environments rather than silently automating a previously gated action.

- [ ] **Step 4: Verify both validators pass**

```bash
node scripts/ci/check-gitlab-parity-manifest.mjs
node scripts/ci/assert-protected-boundaries.mjs
```

Expected: exit 0 for both.

- [ ] **Step 5: Commit**

```bash
git add .gitlab/ci/security.yml ci/gitlab-parity-manifest.json scripts/ci/assert-protected-boundaries.mjs
git commit -m "ci: preserve Guardian protected boundaries on GitLab"
```

---

### Task 4: Import the repository to GitLab and verify refs before enabling GitLab-first development

**Files:**
- Create: `docs/migration/gitlab-import-verification.md`

**Interfaces:**
- Consumes: GitHub repository refs and the newly imported GitLab project.
- Produces: a recorded ref comparison proving the GitLab import contains the expected default branch, migration branch, tags, and representative recent commits.

- [ ] **Step 1: Import the GitHub repository into a private GitLab project**

Use GitLab's repository import flow. Keep the GitHub repository unchanged. The GitLab project should initially use the same repository name, `Buys-Mock`, unless the namespace requires a temporary disambiguation.

- [ ] **Step 2: Add both remotes locally and fetch all refs**

```bash
git remote -v
git remote add gitlab <GITLAB_PROJECT_GIT_URL>
git fetch origin --prune --tags
git fetch gitlab --prune --tags
```

Do not change `origin` yet.

- [ ] **Step 3: Compare branches/tags and representative commits**

```bash
git show-ref --heads --tags | sort > /tmp/github-refs.txt
git ls-remote --heads --tags gitlab | sort > /tmp/gitlab-refs.txt
```

Normalize local-vs-remote formatting and verify that `main`, `migration/gitlab-staged-20260914`, all release tags, and representative recent commit SHAs are present in GitLab.

- [ ] **Step 4: Record the verification result**

Create `docs/migration/gitlab-import-verification.md` containing the GitLab project path, verification date, compared refs, any intentionally excluded provider metadata, and a clear PASS/FAIL conclusion. Do not include credentials or tokens.

- [ ] **Step 5: Commit the verification document**

```bash
git add docs/migration/gitlab-import-verification.md
git commit -m "docs: verify GitLab repository import"
```

---

### Task 5: Configure GitLab CI/CD variables and protected execution

**Files:**
- Create: `docs/migration/gitlab-ci-variable-map.md`

**Interfaces:**
- Consumes: secrets/permissions referenced by GitHub workflows and the parity manifest.
- Produces: GitLab project variables and protections with no secret values committed to git.

- [ ] **Step 1: Build the variable map from workflow references**

For each GitHub workflow, record only the variable name and purpose, not its value. Classify each as one of `masked+protected`, `masked`, or `plain-nonsecret`.

- [ ] **Step 2: Configure GitLab variables**

In GitLab project settings, create the required variables. Signing keys, deployment tokens, Supabase service credentials, and similar privileged values must be `masked+protected`.

- [ ] **Step 3: Protect branches/environments used by privileged jobs**

Protect `main` and release tags. Ensure privileged/manual jobs cannot run from untrusted merge-request branches with protected variables.

- [ ] **Step 4: Document names and protection only**

Create `docs/migration/gitlab-ci-variable-map.md` with columns:

```text
Variable | Purpose | GitHub source workflow | GitLab job | Protection
```

Never record values.

- [ ] **Step 5: Commit documentation**

```bash
git add docs/migration/gitlab-ci-variable-map.md
git commit -m "docs: map GitLab CI variables and protections"
```

---

### Task 6: Run same-revision side-by-side CI and compare outcomes/artifacts

**Files:**
- Create: `scripts/ci/compare-ci-parity.mjs`
- Create: `docs/migration/gitlab-ci-parity-results.md`

**Interfaces:**
- Consumes: GitHub workflow results, GitLab pipeline results, and artifact metadata for the same commit SHA.
- Produces: explicit PASS/FAIL parity evidence for every required manifest item.

- [ ] **Step 1: Write the comparison input validator**

Create `scripts/ci/compare-ci-parity.mjs` so it accepts two JSON files, each containing job name/status/artifact names, and fails when a required manifest mapping is missing, failed on one provider, or expected artifacts differ.

Expected invocation:

```bash
node scripts/ci/compare-ci-parity.mjs /tmp/github-ci.json /tmp/gitlab-ci.json
```

- [ ] **Step 2: Trigger GitHub and GitLab CI for the exact same migration-branch commit SHA**

Do not compare different revisions. Record the SHA in the parity-results document.

- [ ] **Step 3: Capture normalized provider results**

Normalize each provider into:

```json
{
  "sha": "<commit-sha>",
  "jobs": [
    { "name": "job-name", "status": "success", "artifacts": ["artifact-name"] }
  ]
}
```

Do not include logs containing secrets.

- [ ] **Step 4: Run the parity comparison**

```bash
node scripts/ci/compare-ci-parity.mjs /tmp/github-ci.json /tmp/gitlab-ci.json
```

Expected: exit 0 only when all required checks are equivalent or GitLab is stricter.

- [ ] **Step 5: Record artifact/build verification**

In `docs/migration/gitlab-ci-parity-results.md`, explicitly record Android/APK/AAB, Admin/recovery, OTA/release validation, Guardian/security, governance/feature-contract, and general quality results.

- [ ] **Step 6: Commit parity evidence**

```bash
git add scripts/ci/compare-ci-parity.mjs docs/migration/gitlab-ci-parity-results.md
git commit -m "test: record GitLab CI parity evidence"
```

---

### Task 7: Prepare GitLab-primary development cutover while preserving GitHub rollback

**Files:**
- Create: `docs/migration/gitlab-cutover-runbook.md`
- Modify: repository documentation that tells contributors which host is primary, only after parity passes.

**Interfaces:**
- Consumes: successful Task 6 parity evidence.
- Produces: a reversible cutover sequence and rollback instructions.

- [ ] **Step 1: Write the cutover runbook before changing integrations**

The runbook must require all of the following before proceeding:

```text
GitLab import verification = PASS
GitLab CI parity = PASS
Guardian/security parity = PASS
Android/Admin artifacts = PASS
GitHub rollback source verified = PASS
```

- [ ] **Step 2: Switch development workflow, not production deployment, first**

Make GitLab merge requests and GitLab CI the primary development path. Do not yet reconnect Vercel or change Supabase production configuration.

- [ ] **Step 3: Configure GitHub as mirror/rollback**

Set up one-way GitLab -> GitHub mirroring where account permissions support it. Prevent independent feature development on both hosts after cutover.

- [ ] **Step 4: Validate rollback**

Confirm the latest approved GitLab commit is visible on GitHub mirror and that GitHub's `main` remains usable as the fallback source.

- [ ] **Step 5: Commit the runbook/documentation**

```bash
git add docs/migration/gitlab-cutover-runbook.md
git commit -m "docs: add GitLab-primary cutover and rollback runbook"
```

---

### Task 8: Reconnect deployment integrations only after source/CI cutover is stable

**Files:**
- Create: `docs/migration/gitlab-deployment-cutover.md`

**Interfaces:**
- Consumes: stable GitLab-primary repository/CI state plus existing Vercel/Supabase deployment configuration.
- Produces: independently validated deployment integration with a documented rollback path.

- [ ] **Step 1: Inventory current deployment integrations**

Record which production/preview deployments are Git-connected, which are API/CLI driven, and which environment variables are provider-managed. Do not expose values.

- [ ] **Step 2: Reconnect Vercel to GitLab where Git integration is used**

Preserve project, domain, root directory, build command, output settings, and environment-variable scopes. Do not create a duplicate production project unless the existing project cannot be reconnected safely.

- [ ] **Step 3: Leave Supabase backend/data in place**

Only update repository/deployment hooks that genuinely depend on GitHub. Do not migrate database/auth/storage as part of this task.

- [ ] **Step 4: Validate preview then production**

Validate a GitLab-sourced preview deployment first. Only after that passes should the production source integration be switched.

- [ ] **Step 5: Record deployment validation and rollback**

Create `docs/migration/gitlab-deployment-cutover.md` with project identifiers, validation results, and exact rollback direction back to the prior GitHub source. Do not include secrets.

- [ ] **Step 6: Commit**

```bash
git add docs/migration/gitlab-deployment-cutover.md
git commit -m "docs: verify GitLab deployment cutover"
```

---

### Task 9: Final migration verification and merge gate

**Files:**
- Modify: `docs/migration/gitlab-ci-parity-results.md`
- Modify: `docs/migration/gitlab-cutover-runbook.md`

**Interfaces:**
- Consumes: all previous task outputs.
- Produces: final go/no-go evidence for merging migration configuration to the primary branch.

- [ ] **Step 1: Run all repository quality/security gates available on the migration branch**

Run the same commands mapped as `required: true` in `ci/gitlab-parity-manifest.json`, plus:

```bash
node scripts/ci/check-gitlab-parity-manifest.mjs
node scripts/ci/assert-protected-boundaries.mjs
```

Expected: all required gates exit 0.

- [ ] **Step 2: Re-run same-revision GitHub/GitLab parity one final time**

Use the exact final migration-branch SHA and require the comparison script to exit 0.

- [ ] **Step 3: Confirm rollback remains available**

Verify GitHub still contains the production baseline plus the mirrored migration result and has not been archived/deleted.

- [ ] **Step 4: Mark the migration ready only if every success criterion passes**

If any required result differs, do not cut over; continue operating from GitHub `main` while correcting the migration branch.

- [ ] **Step 5: Open the final merge request with evidence links**

The merge request description must link the import verification, CI parity results, variable/protection map, cutover runbook, and deployment validation. Do not merge until all required GitLab checks are green.
