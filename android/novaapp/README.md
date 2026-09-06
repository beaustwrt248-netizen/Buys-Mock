# Nova Android

Standalone Nova Android application package: `com.buysloans.nova`.

## Release channel

Stable builds are published through the protected Nova OTA workflow. The first stable install establishes the production package/signing identity; later stable releases use the in-app OTA channel with SHA-256 verification and Android's install authorization boundary.

## Release safety

- Keep the stable signing identity unchanged once published.
- Do not embed repository, signing, service-role, or other privileged secrets in the APK.
- OTA metadata must only point at immutable `nova-v*` GitHub release assets produced by the protected release workflow.
- Guardian protected actions and Morley security boundaries remain outside the Nova APK.
