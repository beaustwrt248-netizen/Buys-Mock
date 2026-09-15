# OTA and Release Infrastructure Completion Design

## Goal

Finish and standardise the protected Android OTA and release infrastructure for Morley Buys, Nova Next, and Morley Admin without republishing, rewriting, weakening, or replacing already verified releases.

## Current baseline

- Morley Buys 2.15.107 / versionCode 151 is already published through the protected OTA path.
- Nova Next 0.1.1 / versionCode 2 is already published through the protected production workflow and feed promotion.
- Historical Nova Next 0.1.0 / versionCode 1 remains provenance-conflicted/quarantined and must never be rewritten or reused.
- Existing signing identities, protected-main requirements, Guardian boundaries, Auth/RLS boundaries, pricing authority, and production-data authority remain unchanged.
- Morley Admin already has dedicated Android build, release-check, OTA-feed-deploy, and recovery workflows that must be audited against the same release guarantees rather than replaced wholesale.

## Scope

This work audits and closes gaps across three independent release lanes: Morley Buys, Nova Next, and Morley Admin. Shared contracts may be added where they can verify common guarantees without forcing the applications to share release identity, signing configuration, feeds, tags, or package names.

The completion target is a deterministic chain for every active Android app:

1. exact reviewed source;
2. reproducible/traceable build job;
3. expected production signing identity;
4. immutable release identity and asset;
5. SHA-256 binding to the exact APK;
6. monotonic version sequencing;
7. protected OTA metadata promotion;
8. updater discovery and install validation;
9. safe failure before partial/stale publication;
10. documented recovery/rollback procedure that does not rewrite immutable history.

## Architecture

Each app retains its own build and release workflow. We will add or strengthen focused validation contracts around those workflows instead of creating a new central release service. Common invariants will be expressed as repository tests where practical: exact-source provenance, version monotonicity, immutable tag/release behavior, checksum binding, expected signer validation, protected feed promotion, stale-build refusal, and recovery semantics.

Release publication remains two-phase where protection requires it: first produce and verify the immutable signed release artifact, then promote the corresponding OTA metadata through the protected reviewed path. A release asset existing without promoted feed metadata is a safe intermediate state; a feed must never advertise an unverified or mismatched asset.

## Morley Buys lane

Preserve the existing `auto-ota-release.yml` behavior that checks out the exact green build source, rejects version jumps, verifies/reuses only byte-identical immutable release assets, computes SHA-256, and stages metadata through protected main. Audit its companion build/updater/recovery contracts and close only demonstrated gaps.

No existing Morley release or feed entry is republished as part of this infrastructure completion.

## Nova Next lane

Treat the already-promoted 0.1.1 / versionCode 2 release as the current production baseline. Preserve the explicit quarantine of historical versionCode 1 and exact-source tag binding introduced by the provenance repairs. Audit the production workflow, metadata contract, native updater, manual update UX, signer validation, and recovery path as one release lane.

The work must not mutate `nova-next-v0.1.0`, reuse versionCode 1, or publish a new Nova Next release merely to test infrastructure. Future release tests must be non-publishing contracts unless a separately approved real release is required.

## Morley Admin lane

Audit the existing Admin Android build, release-check, OTA-feed-deploy, and recovery workflows against the same invariants. Preserve Admin's package/signing/feed identity. Where Admin lacks a guarantee already enforced for Morley or Nova Next, add the narrowest equivalent protection and regression test.

Do not promote a new Admin production release solely for this audit. Production publication remains a separate protected action if a genuine source release is later required.

## Failure and recovery behavior

All lanes fail closed. A signer mismatch, checksum mismatch, stale source, version regression/jump outside an explicitly quarantined sequence, conflicting immutable release, missing artifact, malformed metadata, or main advancing to a different release identity must stop publication.

Rollback means restoring service through an explicitly supported newer corrective release or documented recovery artifact/path. Existing immutable tags/releases and historical OTA evidence are not rewritten to simulate rollback. Feed rollback to an older version is prohibited unless an existing app-specific recovery design explicitly requires it and repository protections validate it; the default is forward-only correction.

## Testing and evidence

Changes use TDD: first add a regression contract demonstrating each missing guarantee, observe the intended failure, then make the minimum workflow/runtime change required to pass it. Verification includes app-specific release/OTA contracts plus repository security audit, quality gate, parity/feature-contract gates relevant to the changed files, Android lint/unit/build validation where Android source is touched, and exact-head status review before merge.

No production release, tag, signing credential, protected feed, Guardian authority, Auth/RLS policy, pricing authority, or production data is modified merely to obtain test evidence.

## Completion criteria

OTA/release infrastructure is complete when all three active Android lanes have repository-enforced evidence for exact-source provenance, expected signing, immutable artifacts, checksum-bound metadata, valid monotonic sequencing, protected feed promotion, stale/conflict refusal, updater discovery/install contracts, and documented recovery behavior; all changed exact-head checks are green; and any implementation PR is merged only through existing protected-main rules.

## Explicit non-goals

- No redesign of application UI.
- No replacement of current production Nova or Morley websites.
- No signing-key rotation.
- No weakening of branch protection or required checks.
- No direct protected-main writes.
- No rewriting or deletion of historical releases/tags.
- No unnecessary new release publication.
- No unrelated Guardian, Auth/RLS, pricing, catalogue, or production-data changes.
