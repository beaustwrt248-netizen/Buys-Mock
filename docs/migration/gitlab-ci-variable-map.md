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

## Android runner requirement

Android jobs use the GitLab runner tag:

`morley-android`

That runner must provide:

- Java 17
- Gradle 9.3.1
- Node.js 22 (required by Nova Next and Android-adjacent contract jobs)
- Python 3
- Android SDK matching the repository build configuration
- Android build-tools including `apksigner` and `aapt`
- `curl`, `unzip`, `sha256sum`, `keytool`, Git and standard POSIX/Bash utilities
- enough disk/RAM to run Android unit tests, lint and APK builds without cross-job state

The dedicated runner must not expose protected variables to unprotected branches or untrusted merge requests. Runner registration must be locked to the `Buys-Mock` project (or a tightly controlled Morley runner group), and protected-runner mode should be used for signed builds.

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
4. Restrict privileged runners/jobs to protected refs and protected environments.
5. Keep publishing/OTA/deployment writes manual until side-by-side parity passes.
6. Keep Guardian's final merge and protected repair boundaries human-controlled.
7. Do not enable job-token repository push capability for this migration unless a later reviewed workflow explicitly requires it.

## Current migration-state rule

Required read-only validation, security, quality, governance, Android and Admin build jobs are now represented in GitLab CI. Provider-specific **write** automations remain manual and intentionally fail when invoked until authenticated GitLab project protections and release/deployment permissions are verified.

A green read-only pipeline is not sufficient for cutover: import/ref verification, protected variables, runner registration, same-revision artifact parity and rollback verification must also pass.
