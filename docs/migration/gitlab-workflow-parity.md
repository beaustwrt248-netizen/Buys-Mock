# GitHub → GitLab Workflow Parity Baseline

Migration branch: `migration/gitlab-staged-20260914`

This document is the human-readable companion to `ci/gitlab-parity-manifest.json`. Every GitHub workflow is represented in the manifest. Required checks must have command-for-command (or stricter) GitLab behavior before cutover. Provider-specific write/publish automations remain non-required and manual/blocked until protected GitLab execution is configured.

## Safety rule

A missing port is a failing migration gate, not a passing placeholder. Jobs that have not yet been translated are deliberately configured to exit non-zero. GitHub remains the rollback/production source until all required jobs are green for the same revision.

## High-risk workflow parity already captured

| GitHub workflow | Trigger/Scope | Key commands / controls | Artifact / secret behavior | GitLab job | State |
|---|---|---|---|---|---|
| `quality-gate.yml` | push/PR to `main`, manual | `python3 scripts/release_audit.py`; JS `node --check`; Guardian/runtime, ecosystem, Nova loader and auth-client tests; Android palette/auth/signing policy assertions; protected Supabase endpoint checks; OTA JSON; login archive; web shell checks | read-only; uses temporary files only | `test:quality-gate` | BLOCKED until full command set is ported |
| `guardian-platform-check.yml` | PR paths under Guardian/sensitive readiness, manual | Guardian JS syntax; `node tests/guardian-platform.test.cjs`; protected-boundary grep assertions | read-only | `security:guardian-platform` | PORTED |
| `security-audit.yml` | PR/push to `main`, manual | `python3 scripts/security_audit.py`; `python3 scripts/test_auto_review_merge_security.py`; `python3 scripts/privacy_audit.py` | read-only | `security:audit` | PORTED |
| `android-pr-permissions-guard.yml` | PR changes to Android PR workflow/permission checker | `python3 scripts/check_android_pr_permissions.py` | read-only | `security:android-permissions-guard` | PORTED |
| `android-pr-readonly.yml` | PR Android/login-video/build changes | release audit; Firebase non-production fallback; Gradle 9.3.1/JDK17 tests, lint, debug APK | optional `FIREBASE_GOOGLE_SERVICES_JSON`; debug APK retained 7 days | `security:android-pr-readonly` | PORTED |
| `nova-pr-guard.yml` | Nova branch PRs | diff classification; fail closed for credentials/signing/destructive SQL/authority expansion/raw data exposure | read-only | `security:nova-pr-guard` | PORTED with GitLab MR variables |
| `build-apk.yml` | main/PR Android changes; manual release | release audit; version resolution; exact login-video byte/hash validation; Firebase config; Gradle tests/lint; debug/release APK; protected signing; exact signer digest verification; APK video verification; release artifact; separate write/publish flows | `FIREBASE_GOOGLE_SERVICES_JSON`, signing material; verified APK/SHA | `android:build-apk` | BLOCKED until signing/release portion is fully ported |

## Full workflow inventory

The following 44 workflows are mapped in `ci/gitlab-parity-manifest.json`:

`admin-android-check.yml`, `admin-apk-build.yml`, `admin-control-integration.yml`, `admin-device-governance.yml`, `admin-ota-feed-deploy.yml`, `admin-recovery-apk.yml`, `admin-release-check.yml`, `admin-support-audit-check.yml`, `admin-support-governance.yml`, `admin-support-reconcile-check.yml`, `android-pr-permissions-guard.yml`, `android-pr-readonly.yml`, `auto-ota-release.yml`, `auto-review-merge.yml`, `build-apk.yml`, `buyshub-web-auth-origin.yml`, `catalogue-live-sync.yml`, `deploy-admin-pages.yml`, `full-feature-contract.yml`, `guardian-platform-check.yml`, `main-admin-mode-removal-contract.yml`, `morley-ai-assessment-core.yml`, `morley-ai-assessment-schema.yml`, `morley-ecosystem-autopilot.yml`, `morley-restore-point.yml`, `nova-apk-build.yml`, `nova-knowledge-maintenance-contract.yml`, `nova-next-ci.yml`, `nova-ota-metadata-contract.yml`, `nova-pr-guard.yml`, `ota-version-policy.yml`, `private-distribution-readiness.yml`, `publish-2.12.0.yml`, `quality-gate.yml`, `resend-email-contract.yml`, `security-audit.yml`, `support-ticket-query-contract.yml`, `ui-consistency.yml`, `ui-pr-checklist-gate.yml`, `ultimate-parity.yml`, `web-bootstrap-live-contract.yml`, `web-desktop-live-contract.yml`, `web-no-blue-filter.yml`, and `web-smoke.yml`.

## Provider-specific write workflows

These remain non-required during the first parity phase and must not run automatically on GitLab until branch/environment protection and required variables are configured:

- `admin-ota-feed-deploy.yml`
- `auto-ota-release.yml`
- `auto-review-merge.yml`
- `catalogue-live-sync.yml`
- `deploy-admin-pages.yml`
- `morley-ecosystem-autopilot.yml`
- `morley-restore-point.yml`
- `publish-2.12.0.yml`

## Cutover rule

GitLab cannot become primary while any required manifest job is still a deliberate `MIGRATION BLOCKED` job. The same commit must pass GitHub and GitLab required gates, with expected APK/report artifacts present, before production integrations are moved.
