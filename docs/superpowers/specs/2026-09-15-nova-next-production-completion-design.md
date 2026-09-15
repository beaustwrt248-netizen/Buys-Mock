# Nova Next Production Completion Design

## Goal
Nova Next is complete when it is feature-complete and production-ready and the first signed Android stable OTA, version `0.1.0` / versionCode `1`, has been published and independently verified. This completion does not replace or promote Nova Next over the existing `/nova/` production experience.

## Current baseline
Nova Next already has the responsive shell, Chat/Tools/Control Centre completion work, PWA update separation, adaptive Android icon, isolated Android package `com.buysloans.novanext`, stable OTA channel `nova-next`, Android source version `0.1.0` / versionCode `1`, bootstrap OTA metadata `0.0.0` / versionCode `0`, signed-release workflow, package/hash/signature validation, release-tag provenance binding, session-profile hydration, and regression coverage. The production OTA manifest remains intentionally unpublished until the protected release succeeds.

## Completion gates
### 1. Final product and parity audit
Audit Home, Chat, Tools, Tasks, More/Control Centre, Settings and login/runtime transitions at supported responsive widths. Visible clipping, overlap, stale placeholder copy, broken filtering/search, dead controls, route failures, incorrect session identity, inaccessible controls, or internal operational text exposed to ordinary users are blockers. Every discovered defect receives regression coverage before its implementation fix.

### 2. Live-runtime and protected-boundary verification
Verify the existing supported Morley Admin authentication/runtime path and correct session hydration. Ordinary Nova UI must not perform Guardian repair execution, production deployment/promotion, release approval, signing, pricing authority, user/role mutation, Auth/RLS/schema mutation, or destructive production-data actions. No completion change may weaken Guardian, human approval, signing, release, OTA, Auth/RLS, pricing, or production promotion controls.

### 3. Android release readiness
Release identity is fixed: appId `nova-next`, channel `stable`, package `com.buysloans.novanext`, versionName `0.1.0`, versionCode `1`. Verify release build, package/version identity, signing continuity, SHA-256, FileProvider installer handoff, unknown-sources resume, manifest validation, release URL allowlist, unsafe version/path rejection, and rejection of wrong app/channel/package or non-newer versions.

### 4. Repository and security gates
Run the complete Nova Next contracts, JS syntax, Android unit/lint/build, repository security audit, parity/quality gates, path stability and applicable protected-release checks against the exact reviewed source. Required failures block release unless a check is explicitly documented as intentionally skipped for that event. Immutable release tags must target the exact source SHA that produced the verified signed APK; tag/version reuse is forbidden.

### 5. Protected first production OTA publication
Use only the existing manual `workflow_dispatch` Nova Next production OTA mechanism from `main`. Do not emulate publication with direct metadata writes, ad-hoc tags, locally signed artifacts, bypassed checks, or weakened protections. The protected workflow builds/verifies the signed APK, creates the immutable release bound to the build SHA, verifies SHA-256, stages guarded OTA metadata, and uses the approved promotion/handoff path. Guardian/human approval remains mandatory wherever requested.

### 6. Post-publication verification
Completion requires independent evidence that the immutable `0.1.0 (1)` release exists; its tag resolves to the verified source SHA; APK package/signing identity is correct; SHA-256 matches; stable metadata reports appId `nova-next`, channel `stable`, package `com.buysloans.novanext`, versionName `0.1.0`, versionCode `1`, approved release URL and matching SHA; the client accepts it as newer valid metadata and rejects cross-channel/package metadata; installer handoff remains user-mediated with no JavaScript bridge; and PWA updates remain separate from APK OTA. A successful workflow alone is insufficient without independent artifact/metadata verification.

## Failure and rollback behavior
Before metadata promotion, failures abort publication and leave the bootstrap manifest unchanged. After immutable release creation, failures must not reuse or silently retarget its tag. Recovery uses the protected release process and a new monotonically increasing version when required; protections are never bypassed.

## Out of scope
No `/nova/` replacement or redirect, Morley Admin authorization redesign, Auth/RLS redesign, pricing/catalogue authority change, or new privileged Nova production actions.

## Definition of done
Nova Next is complete only when all blocker audits are clean, required tests/gates are green on the exact release source, protected manual signed OTA publication succeeds, and the resulting `0.1.0 (1)` release plus stable metadata pass independent post-publication verification.