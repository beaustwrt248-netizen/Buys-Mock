# Nova Next Completion + OTA Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish Nova Next's mobile UI, launcher identity and isolated OTA update channel without weakening existing protected operational boundaries.

**Architecture:** Preserve the existing Nova Next HTML/CSS/JS and Android wrapper, consolidating layout behavior into the existing shared shell rather than replacing the product. Reuse Morley's OTA transport/release machinery through Nova-specific identity, manifest and validation contracts; native APK OTA and PWA service-worker updates remain separate.

**Tech Stack:** HTML, CSS, vanilla JavaScript, PWA service worker/web manifest, Android Gradle/Java or Kotlin as already present, repository test tooling and GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-15-nova-next-completion-ota-design.md`

## Global Constraints

- Preserve Guardian, human-approval, deployment, pricing, catalogue and user/role protected boundaries.
- Nova and Morley must have distinct OTA application/channel/package identities and version streams.
- Do not replace Nova Next or its existing product architecture.
- Native APK OTA and PWA service-worker updates are independent mechanisms.
- Existing security, parity, feature-contract and quality gates must pass before merge/release.

---

### Task 1: Baseline and regression contracts

**Files:**
- Modify: `nova-next/tests/*`
- Read/verify: `nova-next/index.html`, `nova-next/app.js`, `nova-next/styles.css`, `nova-next/live.css`, `nova-next/accessibility.css`

**Interfaces:**
- Consumes: current DOM routes and Nova shell selectors.
- Produces: failing regression contracts for safe areas, chat structure, tool filtering, icon manifest and OTA isolation.

- [ ] **Step 1:** Inspect current Nova tests and repository test commands; identify the existing DOM/static-contract test style.
- [ ] **Step 2:** Add failing tests asserting one chat scroll viewport, one persistent composer, empty-state-only quick actions, safe-area/bottom-nav padding, functional tool category metadata, manifest icon entries and Nova-specific OTA identity.
- [ ] **Step 3:** Run only the new Nova regression tests and confirm they fail for the intended missing behavior rather than harness errors.
- [ ] **Step 4:** Commit the red tests as `test: define Nova Next completion contracts`.

### Task 2: Shared responsive shell and safe areas

**Files:**
- Modify: `nova-next/index.html`
- Modify: `nova-next/styles.css`
- Modify: `nova-next/live.css`
- Modify: `nova-next/accessibility.css`
- Test: `nova-next/tests/*`

**Interfaces:**
- Consumes: existing page/route identifiers.
- Produces: shared shell classes and CSS inset variables used by every route.

- [ ] **Step 1:** Add/adjust tests for header consistency, `env(safe-area-inset-top)` and `env(safe-area-inset-bottom)`, scroll padding and keyboard/narrow viewport behavior.
- [ ] **Step 2:** Run those tests and verify failure.
- [ ] **Step 3:** Consolidate shell/header/content/bottom-nav structure and define reusable inset/navigation sizing variables; ensure scroll containers reserve bottom navigation plus safe-area space.
- [ ] **Step 4:** Ensure reduced-motion and focus-visible behavior survives the shell consolidation.
- [ ] **Step 5:** Run Nova shell/accessibility tests and commit as `fix: unify Nova Next responsive shell`.

### Task 3: Chat structural repair

**Files:**
- Modify: `nova-next/index.html`
- Modify: `nova-next/app.js`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/*`

**Interfaces:**
- Consumes: shared shell/inset variables from Task 2 and existing chat send/render functions.
- Produces: header -> scrollable message viewport -> persistent composer layout and an explicit empty-chat state.

- [ ] **Step 1:** Add failing tests for long assistant wrapping, composer persistence, quick-action removal after first message, message viewport scrolling and non-error assistant styling.
- [ ] **Step 2:** Run targeted chat tests and verify failure.
- [ ] **Step 3:** Move quick actions into the empty state; keep conversation history inside one scroll viewport; keep composer outside that viewport immediately above bottom navigation.
- [ ] **Step 4:** Replace normal assistant error-red presentation with Nova message styling while preserving genuine error states.
- [ ] **Step 5:** Prevent operational/capability boilerplate from being injected as normal chat content while leaving authorization enforcement untouched.
- [ ] **Step 6:** Run chat tests and commit as `fix: rebuild Nova chat layout states`.

### Task 4: Tools filtering, search and routing

**Files:**
- Modify: `nova-next/index.html`
- Modify: `nova-next/app.js`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/*`

**Interfaces:**
- Consumes: existing tool catalogue and route navigation.
- Produces: canonical tool category metadata and combined `category + search` filtering.

- [ ] **Step 1:** Add failing tests proving All returns every tool, each category returns only matching tools, search composes with category and every rendered tool has a valid destination/action.
- [ ] **Step 2:** Run targeted tests and verify failure.
- [ ] **Step 3:** Define canonical categories for each current tool and implement a single render/filter path for category and search state.
- [ ] **Step 4:** Normalize active/focus states and ensure the last result clears bottom navigation.
- [ ] **Step 5:** Run Tools tests and commit as `fix: make Nova tool discovery functional`.

### Task 5: Home, Tasks and Control Centre polish

**Files:**
- Modify: `nova-next/index.html`
- Modify: `nova-next/app.js`
- Modify: `nova-next/styles.css`
- Test: `nova-next/tests/*`

**Interfaces:**
- Consumes: shared shell and existing telemetry endpoints/state.
- Produces: recognisable Home controls and explicit Control Centre telemetry state model.

- [ ] **Step 1:** Add failing tests for Home action semantics and telemetry `loading|healthy|degraded|unavailable` states plus retry behavior.
- [ ] **Step 2:** Run tests and verify failure.
- [ ] **Step 3:** Normalize Home/Tasks spacing/actions and replace ambiguous glyph controls with labelled/accessible actions.
- [ ] **Step 4:** Render Control Centre telemetry through explicit state classes and retry action; retain read-only/protected operational boundaries.
- [ ] **Step 5:** Run targeted tests and commit as `fix: polish Nova core surfaces`.

### Task 6: Nova launcher/PWA icon

**Files:**
- Create/modify: `nova-next/assets/icons/*`
- Modify: `nova-next/manifest.webmanifest`
- Modify: `nova-next/android/app/src/main/res/mipmap-anydpi-v26/*`
- Modify/create: Android drawable/mipmap density assets under `nova-next/android/app/src/main/res/`
- Test: `nova-next/tests/*`

**Interfaces:**
- Consumes: approved glowing blue-violet Nova orb visual identity.
- Produces: Android adaptive foreground/background launcher resources and PWA standard/maskable icons.

- [ ] **Step 1:** Add failing manifest/resource contract tests for required Android adaptive resources and PWA 192/512 standard + maskable entries.
- [ ] **Step 2:** Run tests and verify failure.
- [ ] **Step 3:** Create the orb master artwork and derive required web/Android launcher resources without embedding secrets or runtime data.
- [ ] **Step 4:** Wire adaptive Android icon XML and PWA manifest entries to the new resources.
- [ ] **Step 5:** Run icon/manifest tests and commit as `feat: add Nova launcher identity`.

### Task 7: Isolated Nova OTA contract

**Files:**
- Inspect/reuse: existing Morley OTA implementation discovered in repository
- Create/modify: Nova-specific OTA client/config files under `nova-next/`
- Modify: `nova-next/android/app/build.gradle`
- Modify: Android manifest/native bridge files under `nova-next/android/app/src/main/`
- Test: `nova-next/tests/*` and existing OTA tests

**Interfaces:**
- Consumes: Morley OTA transport/release contract and Nova Android package/version metadata.
- Produces: Nova-only update descriptor containing application/channel/package/version/hash/download metadata and validation that rejects Morley descriptors/packages.

- [ ] **Step 1:** Locate the existing Morley OTA client, release manifest schema, signing/hash checks and CI publication path; document exact reusable interfaces in the implementation commit.
- [ ] **Step 2:** Add failing tests that accept a correctly identified newer Nova release and reject wrong app id, channel, package, stale version, malformed hash and Morley metadata.
- [ ] **Step 3:** Run OTA isolation tests and verify failure.
- [ ] **Step 4:** Implement Nova-specific OTA configuration and validation while reusing transport primitives; keep Nova versionCode/versionName independent.
- [ ] **Step 5:** Implement native update states: checking, up-to-date, available, downloading, verifying, ready/install handoff, offline/failure and retry.
- [ ] **Step 6:** Run Nova and existing Morley OTA tests to prove isolation and no regression.
- [ ] **Step 7:** Commit as `feat: add isolated Nova OTA channel`.

### Task 8: PWA update separation

**Files:**
- Modify: `nova-next/service-worker.js`
- Modify: `nova-next/app.js`
- Test: `nova-next/tests/*`

**Interfaces:**
- Consumes: current Nova service worker registration.
- Produces: versioned web-asset cache/update behavior that never masquerades as native APK OTA.

- [ ] **Step 1:** Add failing tests for cache versioning, activation cleanup and separation from native OTA state.
- [ ] **Step 2:** Run tests and verify failure.
- [ ] **Step 3:** Implement deterministic Nova web cache versioning/activation and a web-refresh prompt only when a new worker is ready.
- [ ] **Step 4:** Run service-worker and OTA tests together and commit as `fix: separate Nova web and native updates`.

### Task 9: Full verification and PR

**Files:**
- Modify only defects revealed by verification, with a regression test accompanying each fix.

**Interfaces:**
- Consumes: Tasks 1-8.
- Produces: reviewable Nova Next completion branch with evidence from all required gates.

- [ ] **Step 1:** Run the complete Nova Next test suite.
- [ ] **Step 2:** Run repository security, parity, feature-contract and quality gates applicable to Nova/Android/OTA.
- [ ] **Step 3:** Build the Nova Android debug/release-validation target supported by repository CI and verify package/version/icon resources.
- [ ] **Step 4:** Review the final diff for unrelated changes, exposed secrets, weakened protected boundaries and Nova/Morley OTA identity crossover.
- [ ] **Step 5:** Fix any discovered defect test-first and rerun affected plus full gates.
- [ ] **Step 6:** Push final commits and open a clean PR to `main`, documenting test/build evidence and explicitly noting that release/merge remains subject to required protected checks.