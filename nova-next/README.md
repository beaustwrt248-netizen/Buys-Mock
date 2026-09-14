# Nova Next

Nova Next is an isolated successor candidate for Nova AI. It implements the approved dark blue/purple responsive shell while keeping the existing production `nova/` app authoritative until reviewed promotion.

## Current scope

Nova Next now includes the shared responsive shell, repaired Chat layout, searchable/category-filtered Tools, Control Centre state handling, PWA launcher/update behavior, Android package isolation, and a guarded Android OTA path.

The Android package identity is `com.buysloans.novanext`. Nova Next OTA metadata is isolated from the production Nova channel and must match the Nova Next application/channel/package identity before an update is accepted. Downloaded APKs are verified for SHA-256 integrity, package identity, version code and signing certificate before Android installer handoff.

## Local contract tests

```bash
node --test nova-next/tests/*.test.mjs
```

The GitHub Nova Next validation workflow also runs JavaScript syntax checks plus Android unit tests, lint and debug APK assembly.

## Isolation rule

Nova Next must not import frontend/runtime code from `../nova/`. Shared backend behavior is reached only through reviewed adapter contracts and existing protected server-side authorization.

Production promotion, release signing, OTA publication, Guardian repair authority, pricing writes, user/role changes and destructive production-data actions remain protected workflows and are not granted by the Nova Next client.
