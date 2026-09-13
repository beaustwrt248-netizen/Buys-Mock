# GitLab CI Variable and Runner Map

No secret values belong in this document or in the repository.

## Protected CI/CD variables

| Variable | Purpose | GitHub source workflow | GitLab job | Protection |
|---|---|---|---|---|
| `BL_KEYSTORE_BASE64` | Base64-encoded stable Android signing keystore | `build-apk.yml`, `admin-apk-build.yml`, `admin-recovery-apk.yml`, `nova-apk-build.yml` | `android:build-apk`, `admin:apk-build`, `admin:recovery-apk`, `android:nova-apk-build` | masked + protected |
| `BL_KEYSTORE_PASSWORD` | Android keystore password | same signed Android workflows | same signed Android jobs | masked + protected |
| `BL_KEY_ALIAS` | Android signing alias | same signed Android workflows | same signed Android jobs | masked + protected |
| `FIREBASE_GOOGLE_SERVICES_JSON` | Production Firebase Android configuration | `build-apk.yml`, `android-pr-readonly.yml` | `android:build-apk`, `security:android-pr-readonly` | masked + protected for protected builds; optional placeholder on MR validation |

Provider/deployment variables discovered while enabling OTA, Pages, catalogue-sync, Vercel or Supabase-aware write workflows must be added by **name and protection class only**. Values must never be committed.

## Android hosted-runner profile

Android-adjacent parity jobs now use the GitLab.com hosted runner tag:

`saas-linux-medium-amd64`

The migration no longer requires a user-managed runner. The root `.gitlab-ci.yml` pins and verifies the build toolchain inside the hosted job environment:

- Java 17
- Gradle 9.3.1
- Node.js 22.23.2
- Python 3
- Android command-line tools build 14742923
- Android platform 37.0
- Android build-tools 37.0.0
- Android platform-tools
- `curl`, `unzip`, `sha256sum`, `keytool`, Git and standard POSIX/Bash utilities

The hosted profile is applied to `admin:android-check`, `admin:recovery-apk`, `admin:apk-build`, `android:build-apk`, `android:nova-apk-build`, `security:android-pr-readonly`, `test:nova-next-ci`, and `test:ultimate-parity`. Job scripts, rules, artifacts and protected release boundaries remain defined by the included parity jobs.

Authenticated migration evidence for commit `f02a746440467dc406cf28cfc6c5e76e05f39f5c` confirmed that the required hosted Android jobs were scheduled and completed successfully on `saas-linux-medium-amd64`; the earlier `morley-android` capacity blocker is resolved for the staged migration path.

## GitLab predefined/job-token dependencies

The CI configuration also relies on GitLab-provided non-secret variables such as:

- `CI_MERGE_REQUEST_DIFF_BASE_SHA`
- `CI_MERGE_REQUEST_IID`
- `CI_PROJECT_ID`
- `CI_API_V4_URL`
- `CI_JOB_TOKEN`

`governance:ui-pr-checklist` uses the job token only for the read-only Merge Requests API so it can obtain the full MR description and feed the existing UI checklist validator. No personal access token is required for that read-only same-project call.

## Protected execution

Before enabling signed builds or publishing jobs in GitLab:

1. Protect `main`.
2. Protect release tags.
3. Mark signing/deployment variables masked and protected.
4. Restrict privileged jobs to protected refs and protected environments.
5. Keep publishing/OTA/deployment writes manual until side-by-side parity passes.
6. Keep Guardian's final merge and protected repair boundaries human-controlled.
7. Do not enable job-token repository push capability for this migration unless a later reviewed workflow explicitly requires it.
8. Do not make signing variables available to unprotected branches or untrusted merge requests, regardless of hosted-runner availability.

## Current migration-state rule

Required read-only validation, security, quality, governance, Android and Admin build jobs are represented in GitLab CI. Provider-specific **write** automations remain manual and intentionally fail when invoked until authenticated GitLab project protections and release/deployment permissions are verified.

The same-SHA GitLab pipeline `2844956840` completed successfully on commit `f02a746440467dc406cf28cfc6c5e76e05f39f5c`. That resolves runner availability and proves the GitLab read-only CI path can execute, but it does not by itself authorize cutover: protected variables/settings, GitHub-vs-GitLab normalized parity, rollback verification, and the final merge gate must still pass.
