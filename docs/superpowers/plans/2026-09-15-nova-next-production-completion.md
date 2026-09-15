# Nova Next Production Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Finish Nova Next, publish its first protected signed Android stable OTA as `0.1.0` / versionCode `1`, and independently verify the published release.

**Architecture:** Preserve the existing Nova Next web/runtime/Android architecture and protected release pipeline. Completion is a sequence of evidence gates: audit and regression-fix any blockers, verify the exact release candidate, run repository/security gates, perform the existing protected manual publication, then verify immutable release artifacts and stable metadata independently.

**Tech Stack:** HTML/CSS/ES modules, Node contract tests, Android Java/Gradle, GitHub Actions, GitHub Releases, existing Guardian/release controls.

**Spec:** `docs/superpowers/specs/2026-09-15-nova-next-production-completion-design.md`

## Global Constraints
- Release identity: appId `nova-next`, channel `stable`, package `com.buysloans.novanext`, versionName `0.1.0`, versionCode `1`.
- Do not replace or redirect `/nova/`.
- Do not weaken Guardian, human approval, signing, release, OTA, Auth/RLS, pricing, or production-promotion controls.
- Publication must use the existing manual `workflow_dispatch` production OTA workflow from `main`.
- Every discovered product defect receives a failing regression test before its fix.
- Completion requires fresh post-publication evidence; workflow success alone is insufficient.

---

### Task 1: Final Nova Next blocker audit

**Files:**
- Inspect: `nova-next/index.html`
- Inspect: `nova-next/app.js`
- Inspect: `nova-next/completion.css`
- Inspect: `nova-next/live.css`
- Inspect: `nova-next/src/*.mjs`
- Test: `nova-next/tests/*.test.mjs`

**Interfaces:**
- Consumes: current Nova Next UI/runtime behavior on `main`.
- Produces: either a clean blocker report or one TDD fix cycle per confirmed defect.

- [ ] **Step 1: Audit the product surfaces**

Check Home, Chat, Tools, Tasks, More/Control Centre, Settings, login/session transitions, navigation, keyboard/safe-area behavior, search/filter behavior, stale copy, dead controls and ordinary-user exposure of operational text.

- [ ] **Step 2: For each confirmed defect, write a failing focused contract**

Use the existing Node contract style, for example:
```js
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../index.html', import.meta.url), 'utf8');
assert.doesNotMatch(source, /the confirmed stale or broken pattern/);
console.log('nova-completion-blocker-contract: ok');
```

- [ ] **Step 3: Run the focused contract and confirm RED**

Run:
```bash
node nova-next/tests/<new-contract>.test.mjs
```
Expected: FAIL for the confirmed defect, not an unrelated setup error.

- [ ] **Step 4: Implement only the minimal blocker fix**

Modify the owning Nova Next source file without changing protected auth/release policy.

- [ ] **Step 5: Re-run focused and complete Nova contracts**

Run:
```bash
node nova-next/tests/<new-contract>.test.mjs
for f in nova-next/tests/*.test.mjs; do node "$f"; done
```
Expected: PASS.

- [ ] **Step 6: Commit each independently reviewable blocker fix**

```bash
git add nova-next
git commit -m "fix: resolve Nova Next completion blocker"
```

### Task 2: Verify live-runtime and privilege boundaries

**Files:**
- Inspect: `nova-next/src/live-runtime.mjs`
- Inspect: `nova-next/app.js`
- Inspect: `nova-next/index.html`
- Test: existing Nova auth/runtime/protected-boundary contracts under `nova-next/tests/`

**Interfaces:**
- Consumes: existing Morley Admin auth/runtime adapter.
- Produces: evidence that Nova session hydration works and privileged production actions remain inaccessible from ordinary Nova UI.

- [ ] **Step 1: Run all Nova runtime/auth boundary contracts**

```bash
for f in nova-next/tests/*.test.mjs; do node "$f"; done
```
Expected: all PASS.

- [ ] **Step 2: Inspect runtime bindings**

Confirm email/password + Turnstile is the supported login path, authenticated session data hydrates profile/greetings, unsupported auxiliary login controls remain hidden, and no client-side privileged write interface has appeared.

- [ ] **Step 3: Search for prohibited client authority**

```bash
grep -RniE 'addJavascriptInterface|guardian.*(approve|repair)|deploy.*production|role.*update|rls.*(alter|disable)' nova-next --exclude-dir=tests
```
Expected: no newly exposed privileged execution path; investigate any match before proceeding.

- [ ] **Step 4: Record any real defect through Task 1's RED→GREEN cycle**

Do not alter Auth/RLS/Guardian policy merely to make a test pass.

### Task 3: Verify Android release candidate

**Files:**
- Inspect: `nova-next/android/app/build.gradle`
- Inspect: `nova-next/android/app/src/main/AndroidManifest.xml`
- Inspect: `nova-next/android/app/src/main/java/com/buysloans/novanext/UpdateManager.java`
- Inspect: `nova-next/android/app/src/main/java/com/buysloans/novanext/MainActivity.java`
- Inspect: `nova-next/android/app/src/main/res/xml/file_paths.xml`
- Inspect: `nova-next/nova-update.json`
- Test: Android/OTA contracts in `nova-next/tests/`

**Interfaces:**
- Consumes: source release identity `0.1.0 (1)` and bootstrap metadata `0.0.0 (0)`.
- Produces: verified release candidate suitable for the protected workflow.

- [ ] **Step 1: Verify source and bootstrap identities**

```bash
grep -E 'versionCode|versionName|applicationId' nova-next/android/app/build.gradle
cat nova-next/nova-update.json
```
Expected: `com.buysloans.novanext`, source `0.1.0` / `1`, published bootstrap `0.0.0` / `0`.

- [ ] **Step 2: Run Android OTA contracts**

```bash
for f in nova-next/tests/*ota*.test.mjs nova-next/tests/*android*.test.mjs nova-next/tests/*release*.test.mjs; do node "$f"; done
```
Expected: PASS, including identity isolation, URL/path safety and release provenance.

- [ ] **Step 3: Run Android unit/lint/debug build**

```bash
cd nova-next/android
./gradlew test lint assembleDebug
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Inspect installer/security invariants**

Confirm FileProvider is used, downloaded APKs are confined to the approved cache directory, package/version/signing identity is verified before install, unknown-sources flow resumes safely, and there is no JavaScript bridge.

### Task 4: Exact-source repository release gate

**Files:**
- Inspect: `.github/workflows/nova-next-apk-build.yml`
- Inspect: `.github/workflows/nova-next-ci.yml`
- Inspect: repository gate workflows/contracts touched by Nova Next.

**Interfaces:**
- Consumes: exact reviewed candidate SHA.
- Produces: a green, immutable release-source SHA approved for protected publication.

- [ ] **Step 1: Open/update the completion PR from the candidate branch**

Include audit findings, tests run, release identity and explicit statement that production OTA publication is not performed by the PR.

- [ ] **Step 2: Wait for and inspect every required check**

Required Nova contracts, Android build, repository security, parity/quality and path-stability gates must be successful. Event-specific protected publication jobs may be skipped only when their workflow condition intentionally excludes PR events.

- [ ] **Step 3: Resolve review findings with TDD**

For code defects, add RED coverage, implement minimal fix, rerun GREEN, and obtain fresh checks on the new exact head.

- [ ] **Step 4: Merge only after required human/Guardian approval**

Never self-approve a protected change. Capture the resulting exact `main` SHA as the release source.

### Task 5: Protected signed OTA publication

**Files:**
- Execute only: `.github/workflows/nova-next-apk-build.yml`
- Verify output: `nova-next/nova-update.json` promotion artifact/path defined by the workflow.

**Interfaces:**
- Consumes: approved exact `main` release-source SHA.
- Produces: immutable signed `0.1.0 (1)` GitHub Release and guarded stable OTA metadata handoff.

- [ ] **Step 1: Dispatch the existing Nova Next production OTA workflow manually from `main`**

Use GitHub Actions `workflow_dispatch` for `.github/workflows/nova-next-apk-build.yml`. Do not substitute a direct metadata commit, local APK, ad-hoc release, or tag.

- [ ] **Step 2: Verify the workflow checked out the exact approved release SHA**

Expected: release job source SHA equals the captured Task 4 `main` SHA.

- [ ] **Step 3: Verify signing, package, version and SHA steps pass**

Expected: signed release APK is package `com.buysloans.novanext`, versionName `0.1.0`, versionCode `1`; signer check and SHA-256 verification succeed.

- [ ] **Step 4: Verify immutable release creation**

Expected: the new release tag is created once and explicitly targets `$GITHUB_SHA`; existing-tag reuse aborts rather than retargeting.

- [ ] **Step 5: Complete only the existing protected metadata promotion/handoff**

If Guardian or GitHub asks for human approval, stop at that gate and obtain it. Do not bypass it.

### Task 6: Independent post-publication verification

**Files:**
- Read: published GitHub Release/tag/artifacts.
- Read: `nova-next/nova-update.json` from production `main` after approved promotion.
- Test: `nova-next/tests/*ota*.test.mjs` and release provenance contracts.

**Interfaces:**
- Consumes: published release and stable metadata.
- Produces: completion evidence for Nova Next `0.1.0 (1)`.

- [ ] **Step 1: Resolve the published tag**

Expected: tag target equals the exact Task 4/5 release-source SHA.

- [ ] **Step 2: Download/inspect the published release assets through the approved GitHub artifact/release path**

Expected: APK and `.sha256` assets exist and the calculated APK SHA-256 equals the published value.

- [ ] **Step 3: Verify stable metadata independently**

Expected JSON fields:
```json
{
  "appId": "nova-next",
  "channel": "stable",
  "packageName": "com.buysloans.novanext",
  "versionCode": 1,
  "versionName": "0.1.0"
}
```
Also require the approved Nova Next GitHub release URL and matching SHA-256.

- [ ] **Step 4: Re-run OTA acceptance/isolation contracts against final source**

```bash
for f in nova-next/tests/*ota*.test.mjs nova-next/tests/*release*.test.mjs; do node "$f"; done
```
Expected: PASS.

- [ ] **Step 5: Verify client update behavior**

Confirm a versionCode `0`/older Nova Next client sees `1` as newer and valid, while wrong appId/channel/package, bad hash/signature, unsafe URL/path or non-newer version remains rejected. Installer handoff must remain user-mediated.

- [ ] **Step 6: Record completion evidence**

Update the canonical Nova Next status documentation with release tag, exact source SHA, workflow run, SHA-256 verification, metadata identity and final gate results. Do not state completion before these values are verified.

- [ ] **Step 7: Commit documentation through normal PR protections**

```bash
git add docs nova-next
 git commit -m "docs: record Nova Next production completion"
```
