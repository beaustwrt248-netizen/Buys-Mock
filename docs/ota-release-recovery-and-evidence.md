# OTA release recovery and completion evidence

This document is the operational recovery and evidence map for the three Android OTA lanes in this repository: Morley Buys, Nova Next, and Morley Admin. It complements the release workflows; it does not authorize bypassing protected-main checks, signer checks, provenance checks, or human approval boundaries.

## Recovery policy

Recovery is forward-only by default. Never delete, move, retag, rebuild, rewrite, replace, or reuse an immutable production release identity to make a failed publication appear valid. Never repoint an existing immutable tag to another commit. Never replace a released APK with different bytes under the same release identity.

If an immutable asset exists but metadata promotion failed, first verify the tag resolves to the exact tested source, the expected asset exists, its SHA-256 matches the verified build, and the signer/package identity is correct. Only then retry the protected metadata-promotion path. If any of those checks disagree, stop and create a new corrected version/release identity through the normal lane rather than mutating history.

If metadata is wrong but the immutable asset is valid, repair metadata by a normal protected-main PR and rerun the same validation/deployment gates. Do not edit the production feed out of band. If main has advanced to a different app version, do not promote stale metadata; build and publish the current version instead.

If signing identity, package identity, checksum, source provenance, version sequencing, or immutable identity is ambiguous, fail closed. Do not bypass the check, disable branch protection, weaken a regression, or publish an unsigned/re-signed substitute.

Nova Next `nova-next-v0.1.0` / versionCode `1` remains permanently quarantined because its provenance is conflicted. It must not be deleted, moved, rebuilt, rewritten, reused, or republished. Recovery must move forward with a new verified version identity.

## Morley Buys evidence map

- Build/test gate: `.github/workflows/auto-ota-release.yml` and the repository security/quality workflows executed before protected promotion.
- Signing identity: `.github/workflows/auto-ota-release.yml` verifies the stable release signer before publication.
- Checksum: `.github/workflows/auto-ota-release.yml` computes/verifies the release SHA-256 before metadata publication.
- Immutable asset provenance: `.github/workflows/auto-ota-release.yml` binds the release tag to the exact tested source; `tests/morley-ota-release-provenance.test.mjs` locks that contract.
- Metadata publication: `.github/workflows/auto-ota-release.yml` performs the protected OTA metadata handoff rather than bypassing `main`.
- Updater validation: `android/app/src/main/java/com/buysloans/hub/UpdateManager.kt`, `UpdateCheckWorker.kt`, `UpdateActivity.kt`, and `MandatoryUpdateActivity.kt` are the runtime update path; `OtaFeaturePolicy.kt` / `OtaFeatureRules.kt` contain OTA feature policy.
- Recovery: preserve the immutable release; repair/retry only through the normal protected release/metadata lane after exact-source, signer, checksum, package, and version validation. If identity is conflicted, advance to a new version.

## Nova Next evidence map

- Build/test gate: `.github/workflows/nova-next-apk-build.yml` is the signed Nova Next build/release lane and runs its release safety checks before publication.
- Signing identity: `.github/workflows/nova-next-apk-build.yml` verifies the expected Nova Next signer before publication.
- Checksum: `.github/workflows/nova-next-apk-build.yml` verifies the staged release checksum before promotion.
- Immutable asset provenance: `.github/workflows/nova-next-apk-build.yml` binds `NOVA_NEXT_RELEASE_TAG` to the exact tested `GITHUB_SHA` before reuse/create and re-verifies it after publication; `tests/nova-next-ota-release-provenance.test.mjs` locks the contract.
- Metadata publication: `.github/workflows/nova-next-apk-build.yml` publishes the OTA metadata only after immutable-release verification and through the protected release flow.
- Updater validation: `nova-next/android/app/src/main/java/com/buysloans/novanext/UpdateManager.kt`, `UpdateCheckWorker.kt`, `UpdateActivity.kt`, and `MandatoryUpdateActivity.kt` are the Nova Next runtime update path; `OtaFeaturePolicy.kt` / `OtaFeatureRules.kt` contain its OTA feature policy.
- Recovery: never touch the quarantined `nova-next-v0.1.0`. For a failed current/future release, retain immutable history, repair metadata only after exact-source/signer/checksum validation, or advance to a new version if the release identity is conflicted.

## Morley Admin evidence map

- Build/test gate: `.github/workflows/admin-apk-build.yml` runs Admin unit tests/lint and builds the signed release artifact; protected repository checks remain authoritative before metadata merge.
- Signing identity: `.github/workflows/admin-apk-build.yml` verifies certificate SHA-256 `c919385ba51d241cd932b44c1fcfcda48529aef5fd4dda0f3b5f8c5f6056ab7b` for the stable Admin release. `.github/workflows/admin-recovery-apk.yml` verifies the same signer for the separate recovery package.
- Checksum: `.github/workflows/admin-apk-build.yml` verifies the release APK checksum before immutable publication; `.github/workflows/admin-recovery-apk.yml` emits the recovery APK SHA-256.
- Immutable asset provenance: `.github/workflows/admin-apk-build.yml` binds the Admin release tag to the exact tested source before reuse/create and re-verifies it before metadata promotion. `tests/admin-ota-release-provenance.test.mjs` locks that contract. The recovery lane explicitly targets its immutable release at the tested `GITHUB_SHA` and verifies the resulting tag; `tests/admin-recovery-release-provenance.test.mjs` locks that contract.
- Metadata publication: `.github/workflows/admin-apk-build.yml` writes `admin/admin-update.json` only after exact immutable-release verification, rechecks current `main`, opens a protected-main PR, waits for required checks, and deploys/compares the live feed. `.github/workflows/admin-ota-feed-deploy.yml` and `.github/workflows/admin-release-check.yml` provide additional feed/release validation paths.
- Updater validation: `android/adminapp/src/main/java/com/buysloans/adminapp/AdminUpdateManager.kt`, `AdminUpdateCheckWorker.kt`, and `AdminUpdateActivity.kt` are the Admin runtime updater path.
- Recovery: `.github/workflows/admin-recovery-apk.yml` builds a separately packaged signed recovery APK for legacy/debug package conflicts. It installs alongside the production Admin package and does not change the production OTA package identity. Existing recovery release identities are never reused; conflicted recovery publication advances to a new version.

## Completion gate

A lane is release-ready only when the build/test gate, signer identity, checksum, immutable release provenance, metadata publication, updater validation, and documented recovery behavior all agree on one release identity. The shared CI workflow `.github/workflows/ota-release-contracts.yml` runs the static provenance contracts for Morley Buys, Nova Next, Admin production OTA, and Admin recovery publication.

Production publication remains intentionally separate from test evidence. Tests and documentation do not create a release, alter a feed, or authorize bypassing protected-main controls.
