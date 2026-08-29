# Private repository migration readiness

This document prepares Admin/OTA distribution for a future move away from public-repository resources. It does **not** change repository visibility, live OTA metadata, release URLs, versioning, or production hosting.

## Current public coupling

The current Admin release-control surface intentionally still reads OTA metadata and Android source version data from the public repository, and validates APK URLs against the repository's public GitHub Releases prefix. GitHub Pages also remains the current web/Admin deployment path.

These public dependencies are treated as temporary compatibility defaults while migration preparation continues. The readiness build now inventories each detected GitHub Releases, raw GitHub, or GitHub Pages dependency into `public-resource-dependencies.json`. Each entry is explicitly marked as blocking the future private-repository cutover until a private/authenticated replacement exists.

## Portable readiness bundle

The `Private Distribution Readiness` workflow creates `morley-private-distribution-readiness`, a checksum-backed artifact containing:

- the static `admin/` bundle;
- the current checked-in `ota/latest.json` metadata;
- `distribution-manifest.json` with source file hashes, sizes, current published OTA identity, and the number of blocking public-resource dependencies;
- `public-resource-dependencies.json`, a machine-readable inventory of remaining public GitHub distribution coupling;
- `private-origin-contract.json`, a non-live contract requiring HTTPS, authenticated distribution, checksum verification, no client/service-role secrets, and a staged cutover order;
- `SHA256SUMS` covering the staged bundle.

The workflow has `contents: read` only, disables persisted checkout credentials, uses immutable Action SHAs, verifies the bundle checksums, validates that the dependency inventory and private-origin contract agree with the manifest, and explicitly verifies that live public distribution inputs remain untouched during this preparation phase.

`migrationReady` is deliberately derived from the inventory rather than manually declared. It remains `false` while any known public-repository distribution dependency is present and becomes `true` only when the inventory is empty.

## Migration sequence

1. Provision an authenticated/private distribution origin separate from public GitHub Pages/raw content.
2. Publish the generated Admin/OTA bundle to that origin without changing the live client configuration.
3. Add a controlled endpoint-configuration layer with an allowlist and TLS-only URLs; retain the existing public endpoints as rollback defaults during staged testing.
4. Validate Admin authentication/authorization, OTA metadata integrity, APK SHA-256 verification, and rollout health against the private origin.
5. Add signed release metadata or equivalent tamper-evident verification before switching production consumers.
6. Switch Admin distribution first, then OTA metadata, then APK download origin in separate regression-tested PRs.
7. Re-run the readiness build and require `migrationReady: true` with zero blocking public-resource dependencies.
8. Only after all consumers use private/authenticated resources should repository visibility be reconsidered.

## Authentication security preparation

Supabase's security advisor currently reports leaked-password protection as disabled. Enabling that setting is an Auth control-plane change rather than a repository migration, so this lane prepares for it but does not silently change the production Auth configuration. Before enablement, verify the sign-in/reset flows and communicate any password-reset impact; then enable Supabase leaked-password protection as a separately reviewed operational change and re-run the Auth regression checks.

## Explicit non-goals for this phase

- Do not change repository visibility.
- Do not modify live OTA/versioning.
- Do not alter NFC or Valuation 3.0.
- Do not change support-ticket UI owned by its separate lane.
- Do not introduce service-role credentials into Android, browser JavaScript, or GitHub artifacts.
- Do not weaken checksum validation or Actions least-privilege controls.
