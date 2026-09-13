# GitLab CI Variable and Runner Map

No secret values belong in this document or in the repository.

## Protected CI/CD variables

| Variable | Purpose | GitHub source workflow | GitLab job | Protection |
|---|---|---|---|---|
| `BL_KEYSTORE_BASE64` | Base64-encoded stable Android signing keystore | `build-apk.yml`, `admin-apk-build.yml` | `android:build-apk`, `admin:apk-build` | masked + protected |
| `BL_KEYSTORE_PASSWORD` | Android keystore password | `build-apk.yml`, `admin-apk-build.yml` | `android:build-apk`, `admin:apk-build` | masked + protected |
| `BL_KEY_ALIAS` | Android signing alias | `build-apk.yml`, `admin-apk-build.yml` | `android:build-apk`, `admin:apk-build` | masked + protected |
| `FIREBASE_GOOGLE_SERVICES_JSON` | Production Firebase Android configuration | `build-apk.yml`, `android-pr-readonly.yml` | `android:build-apk`, `security:android-pr-readonly` | masked + protected for protected builds; optional on MR validation |

Additional provider/deployment variables discovered while porting the remaining OTA, Pages, catalogue-sync and Supabase-aware workflows must be appended by name and protection class only. Values must never be committed.

## Android runner requirement

Android jobs use the GitLab runner tag:

`morley-android`

That runner must provide:

- Java 17
- Gradle 9.3.1
- Android SDK matching the repository build configuration
- Android build-tools including `apksigner`
- Python 3
- `unzip`, `sha256sum`, `keytool`, Git and standard POSIX shell utilities

The dedicated runner must not expose protected variables to unprotected branches or untrusted merge requests.

## Protected execution

Before enabling signed builds or publishing jobs in GitLab:

1. Protect `main`.
2. Protect release tags.
3. Mark signing/deployment variables masked and protected.
4. Restrict privileged jobs to protected refs/environments.
5. Keep publish/OTA/deployment actions manual until side-by-side parity passes.
6. Keep Guardian's final merge and protected repair boundaries human-controlled.

## Migration-state rule

Any job still emitting `MIGRATION BLOCKED` is deliberately incomplete and must prevent GitLab-primary cutover. This is safer than substituting a weaker check just to obtain a green pipeline.
