# GitLab Import Verification

## Project

- GitHub source: `beaustwrt248-netizen/Buys-Mock`
- GitLab destination: `gitlab.com/beaustwrt248-netizen/Buys-Mock`
- Verification date: 2026-09-14
- GitLab visibility: private

## Current result

**Status: AUTHENTICATED REF AND READ-ONLY CI VERIFICATION PASSED**

The private GitLab project is reachable from the migration bridge through a project-scoped credential stored only as a GitHub Actions secret. The bridge does not print or persist the token.

Authenticated bridge evidence has confirmed:

- GitLab `main` matches GitHub `main` at bridge time.
- `migration/gitlab-staged-20260914` can be pushed to GitLab and its resulting SHA is verified after push.
- GitLab tag refs match GitHub tag refs.
- No force-push is used by the bridge.
- A same-SHA GitLab pipeline can execute successfully on GitLab.com hosted runners without a user-managed runner.

The initial synchronized pipeline for `28e01a15475158994a787114f9bfe928f31ee722` was blocked by GitLab account verification. After account verification, later pipelines exposed a missing `morley-android` self-managed runner dependency. The migration was changed to use the GitLab.com hosted `saas-linux-medium-amd64` runner profile with a pinned Android/Java/Gradle/Node toolchain.

For commit `f02a746440467dc406cf28cfc6c5e76e05f39f5c`, bridge run `34775843567` verified refs and tags, found GitLab pipeline `2844956840` for the exact synchronized SHA, and observed that pipeline complete with status `success`. Required hosted Android jobs including `admin:android-check`, `test:nova-next-ci`, and `test:ultimate-parity` completed successfully. Provider-specific write jobs remained manual.

## Required ref checks

Before cutover, verify all of the following against GitHub:

- `main` exists in GitLab and points to the expected imported commit. **Verified by migration bridge.**
- `migration/gitlab-staged-20260914` exists in GitLab and matches the pushed GitHub migration revision. **Verified by migration bridge.**
- Release tags expected from GitHub are present in GitLab. **Verified by migration bridge.**
- Representative recent commit SHAs exist in both providers. **Partially verified by exact migration-branch and main SHA checks; broader representative sample remains pending.**
- No history rewrite or force-push occurred during migration. **Bridge uses non-force migration-branch pushes only.**

## Verification commands used by the bridge

The bridge performs authenticated ref checks equivalent to:

```bash
git fetch origin main --tags --force
git ls-remote gitlab refs/heads/main
git push gitlab "HEAD:refs/heads/migration/gitlab-staged-20260914"
git ls-remote gitlab refs/heads/migration/gitlab-staged-20260914
git ls-remote --tags origin | sort
git ls-remote --tags gitlab | sort
```

The bridge verifies exact SHA equality after pushing the migration branch and leaves `main` and tags untouched.

## Provider metadata

GitHub-specific Actions run history, workflow artifacts, review metadata, and other provider-only metadata are not considered Git object parity. Source history, branches, tags, and repository content are the migration-critical refs.

## Conclusion

**IMPORT/REF VERIFICATION: PASS. GITLAB READ-ONLY CI EXECUTION: PASS. CUTOVER: STILL NO-GO.** GitHub remains the authoritative rollback source and production baseline until protected GitLab settings/variables, normalized GitHub-vs-GitLab CI parity, rollback verification, and the final merge/cutover gate are completed.
