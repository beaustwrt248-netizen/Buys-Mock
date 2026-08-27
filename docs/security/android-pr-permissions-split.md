# Android workflow least-privilege split

This staged change introduces a dedicated pull-request validation workflow with `contents: read` only.

## Phase 1

- Run release-readiness audit, Android unit regressions, lint, and debug APK compilation under a read-only token.
- Pin all external actions to immutable commit SHAs.
- Do not expose production signing credentials to this validation workflow.
- Do not publish releases or OTA metadata.

## Phase 2 after Phase 1 is green

Remove the `pull_request` trigger and PR-only build step from `build-apk.yml`. That leaves the existing write-capable workflow exclusively for trusted `main`/manual release duties such as verified login-video promotion and explicitly authorized release/OTA publication.

This two-phase rollout avoids disabling the existing PR gate until the replacement read-only gate has proved green on the repository itself.
