# Nova Next Android and Web Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Verify and harden Nova Next Android WebView parity with the deployed web client while preserving the isolated development identity.

**Architecture:** Keep the Android wrapper minimal and web-driven. Add only wrapper-level handling needed for safe file/camera/navigation/keyboard behavior; do not change production package/signing or OTA paths.

**Tech Stack:** Android WebView/Kotlin/Gradle, existing Nova Next web bundle, Node contracts, Android unit/lint/debug build.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-completion-design.md`

## Global Constraints
- Development applicationId remains `com.buysloans.novanext` with debug suffix `.debug`.
- No production signing, package cutover, release or OTA changes.
- Web source stays `/nova-next/`.
- No JavaScript bridge is introduced.
- TDD/static contracts first.

---

### Task 1: Wrapper identity and navigation contracts
**Files:**
- Modify if needed: `nova-next/tests/android-wrapper-contract.test.mjs`
- Modify if needed: `nova-next/android/app/src/main/AndroidManifest.xml`
- Modify if needed: `nova-next/android/app/src/main/java/com/buysloans/novanext/MainActivity.kt`

- [ ] **Step 1:** Extend failing contracts for applicationId/namespace, `/nova-next/` URL, allowed http(s) navigation, no JS bridge, no production signing and safe external intent handling.
- [ ] **Step 2:** Run Node contract and Android unit test task; confirm only genuine gaps fail.
- [ ] **Step 3:** Make the minimum wrapper fixes required for those contracts.
- [ ] **Step 4:** Re-run contracts and commit `fix(nova-next): harden android wrapper parity`.

### Task 2: File/camera and keyboard behavior
**Files:**
- Modify if needed: `nova-next/android/app/src/main/java/com/buysloans/novanext/MainActivity.kt`
- Modify: `nova-next/tests/android-wrapper-contract.test.mjs`

- [ ] **Step 1:** Add failing contract for chooser MIME restrictions, temporary camera URI handling, cancellation cleanup, keyboard resize behavior and no broad storage/camera permission requirement.
- [ ] **Step 2:** Run tests and confirm RED only where behavior is missing.
- [ ] **Step 3:** Implement minimal safe fixes using existing FileProvider/chooser pattern.
- [ ] **Step 4:** Run Android unit tests and commit `fix(nova-next): align mobile file and keyboard behavior`.

### Task 3: Full validation and APK
**Files:** no production changes unless validation finds a low/medium-risk wrapper bug.

- [ ] **Step 1:** Run `node --test nova-next/tests/*.test.mjs` and JS syntax checks.
- [ ] **Step 2:** Run from `nova-next/android`: `./gradlew test lint assembleDebug`.
- [ ] **Step 3:** Compute SHA-256 of `app/build/outputs/apk/debug/app-debug.apk` and record exact artifact path/hash in PR notes.
- [ ] **Step 4:** Verify debug APK loads `/nova-next/`, maintains in-app navigation, file/camera handoff and sign-in page rendering.
- [ ] **Step 5:** Commit any bounded fixes and open a low/medium-risk PR; do not promote production package/signing.
