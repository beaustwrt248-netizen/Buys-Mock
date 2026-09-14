# Repository path stability

This repository has accumulated many long-lived feature, repair, release, and migration branches. A file being absent from one branch does not mean it has been deleted from the project.

## Canonical application baseline

Until the staged GitLab migration is explicitly cut over, `main` is the canonical application/runtime baseline. Migration-only CI and documentation belong on `migration/gitlab-staged-20260914` until they are reviewed and merged.

## Safety rules

- Do not force-push or rewrite `main` or the staged migration branch to reconcile paths.
- Do not delete historical branches as part of path cleanup. Unique work must be audited first.
- Do not silently move or rename canonical runtime entrypoints. Update all callers, tests, workflows, documentation, and this path contract in the same reviewed change.
- When a task reports a missing file, verify the branch/ref first, then search the canonical tree before concluding the file was deleted.
- Keep migration work additive and isolated from production runtime files unless a separately reviewed runtime change requires otherwise.

## Recovery anchors created before reconciliation

- `safety/pre-reconcile-main-20260914`
- `safety/pre-reconcile-migration-20260914`

These branches preserve the exact pre-reconciliation heads and must not be moved during this cleanup.

## Automated guard

`.github/workflows/repository-path-stability.yml` runs `scripts/ci/check-repository-path-stability.mjs` on pull requests and pushes to `main`. The guard verifies critical Admin/workflow paths and checks that local assets referenced by the Admin entrypoints still exist after query-string/version suffixes are removed.

The purpose is to turn accidental path drift into an immediate review failure instead of a later runtime or automation `file not found` error.
