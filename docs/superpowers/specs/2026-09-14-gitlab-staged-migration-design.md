# GitLab Staged Migration Design

## Summary

Migrate `beaustwrt248-netizen/Buys-Mock` from GitHub-primary development to GitLab-primary development using a staged dual-platform approach. GitHub remains intact as a rollback source and mirror until GitLab proves equivalent for source control, merge requests, CI, Android build artifacts, Guardian/security checks, and release validation.

## Goals

- Preserve all Git history, branches, and tags.
- Keep `main` and production behavior unchanged during migration validation.
- Recreate the critical GitHub Actions behavior in GitLab CI rather than redesigning product functionality.
- Verify Android/APK, Admin, OTA, Guardian/security, governance, and release checks before cutover.
- Keep GitHub available as a rollback source during the validation period.
- Change Vercel and Supabase integration only after repository and CI parity is demonstrated.

## Non-goals

- No product redesign.
- No unrelated refactoring.
- No deletion or archival of the GitHub repository during the migration phase.
- No production deployment cutover before parity checks pass.
- No replacement of Supabase as part of this migration.

## Migration Architecture

### Phase 1 — Isolated preparation

All migration work is performed on `migration/gitlab-staged-20260914` or its GitLab equivalent. The existing GitHub `main` branch remains the production baseline.

Inventory the existing `.github/workflows` jobs and group them by responsibility:

1. Android validation and APK builds.
2. Admin validation/build/recovery workflows.
3. OTA feed and release automation.
4. Guardian/security and permissions checks.
5. Governance, support, and feature-contract validation.
6. General test and quality gates.

The GitLab pipeline will preserve the commands and gates that enforce these responsibilities. Platform-specific GitHub plumbing may change, but the underlying checks must remain equivalent.

### Phase 2 — GitLab repository import

Import the GitHub repository into a private GitLab project using GitLab's repository import flow. The imported project must include complete commit history, branches, and tags. GitHub remains unchanged.

After import, compare the GitHub and GitLab refs before any GitLab-first development begins.

### Phase 3 — GitLab CI parity

Add a root `.gitlab-ci.yml` that orchestrates focused CI fragments under `.gitlab/ci/` where useful. Jobs should be grouped into stages such as:

- `validate`
- `test`
- `security`
- `build`
- `release-check`

Reuse repository scripts and existing build commands instead of duplicating business logic inside CI YAML.

GitLab artifacts must retain APK/AAB outputs and any release-validation reports needed by the existing release process.

Secrets, tokens, signing material, and deployment credentials must be configured as protected/masked GitLab CI/CD variables rather than committed to the repository.

### Phase 4 — Side-by-side validation

For the same source revision, run GitHub Actions and GitLab CI and compare:

- required test results;
- Guardian/security outcomes;
- Android build success;
- APK/AAB artifacts where applicable;
- Admin build/recovery checks;
- OTA/release validation behavior;
- feature-contract/governance checks.

Any difference that weakens an existing protection is a migration blocker.

### Phase 5 — GitLab becomes primary

Only after parity is demonstrated:

- use GitLab merge requests for new development;
- make GitLab CI the required pre-merge pipeline;
- reconnect deployment integrations to GitLab where appropriate;
- retain GitHub as a mirror/rollback source.

Production deployment configuration must be changed independently and validated after source/CI parity. This avoids coupling repository migration with deployment migration.

## GitHub Mirror Strategy

During the stabilization period, GitHub stays available as a safety copy. The preferred final state is GitLab-primary with GitHub mirrored from GitLab rather than independent development happening on both platforms.

Direct development on both hosts after cutover is prohibited because it creates divergent histories.

## Safety and Rollback

- Never force-push or rewrite `main` as part of migration.
- Never remove existing GitHub workflows before their GitLab equivalents are validated.
- Never expose signing credentials or deployment secrets in repository files or job logs.
- Preserve Guardian's human-approval and protected-repair boundaries.
- If GitLab parity fails, continue operating from GitHub `main` while fixing the migration branch.
- Vercel/Supabase production configuration remains unchanged until explicitly migrated and verified.

## Success Criteria

The GitLab migration is ready for cutover when all of the following are true:

1. GitLab contains the expected repository history, branches, and tags.
2. Critical GitHub workflow responsibilities have mapped GitLab CI jobs.
3. Required tests and Guardian/security checks pass on GitLab for the same revision that passes on GitHub.
4. Android/Admin build artifacts are produced successfully where expected.
5. Release and OTA validation behavior is equivalent or stricter.
6. No production secrets are committed or printed.
7. GitHub remains available as a tested rollback source.
8. Production deployment is not switched until a separate deployment validation succeeds.

## Initial Scope

The first repository migrated is `beaustwrt248-netizen/Buys-Mock`. Other repositories (`reseller-inventory`, `recovery-tool`, `scrap-yard`, and `pc-price-calculator`) are outside this first implementation and will only be migrated after the Buys-Mock process has been proven.
