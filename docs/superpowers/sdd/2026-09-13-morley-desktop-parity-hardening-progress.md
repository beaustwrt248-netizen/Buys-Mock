# SDD ledger — plan: docs/superpowers/plans/2026-09-13-morley-desktop-parity-hardening.md

Execution note: connector-only environment; no local git worktree is available, so the isolated GitHub branch `hardening/morley-desktop-parity-20260913` is the workspace boundary.

Pre-flight scan:

| Tasks | Shared file/interface | Finding |
| --- | --- | --- |
| 1 ↔ 2 | `morley-desktop-rebuild.js`, capability resolver | Clean: Task 2 consumes Task 1 resolver. |
| 1 ↔ 3 | `morley-desktop-rebuild.js`, capability resolver | Clean: shell controls consume Task 1 resolver. |
| 1 ↔ 4 | `morley-desktop-rebuild.js`, shell state | Clean: Task 4 consumes resolver-owned shell. |
| 1 ↔ 5 | `morley-desktop-rebuild.js`, lifecycle | Clean: Task 5 hardens lifecycle after resolver changes. |
| 2 ↔ 3 | `morley-desktop-rebuild.js`, navigation | Clean: distinct behaviors on same adapter. |
| 3 ↔ 4 | `refreshDesktopState()` | Clean: Task 3 introduces state path, Task 4 expands it. |
| 4 ↔ 5 | refresh/listener lifecycle | Clean: Task 5 makes Task 4 refresh idempotent. |
| 1 | tests vs implementation | Clean: tests require resolver before route rewrite. |
| 2 | tests vs implementation | Clean. |
| 3 | tests vs implementation | Clean. |
| 4 | tests vs implementation | Clean. |
| 5 | tests vs implementation | Clean. |
| 6 | gate verification | Clean. |
| 7 | protected merge/deploy | Clean; merge remains a stop condition if protected. |

Task 1 RED commit: `725e407b5f736709f0a6d1e489f02d037594090b` — new capability ownership contract added before production changes.

Task 1: complete — explicit capability resolver owns desktop feature handoff.
Task 2: complete — Search & Scan and category routing use truthful search/scanner/catalogue paths.
Task 3: complete — AI Insights, Reports, notifications, account and support have explicit behavior and no hard-coded notification badge.
Task 4: complete — desktop-owned KPI/activity/account/notification state refreshes without replacing parked legacy Home DOM; focus-visible and insight styling added.
Task 5: complete — boot, observer and shell/dashboard binding are idempotent while the >=1000px and physical-phone exclusions remain intact.

Ruling: reconcile the hardening branch with current `main` before final PR gates — latest main added Nova/operations files only and did not touch the seven desktop hardening paths, so carrying the hardening blobs onto the current main tree preserves both lines of work without conflict.

Task 6 gate pass on reconciled head `72541050d8fa7c3cb6bfc6be74b1397fe28da7ed`: B&L Morley Quality Gate, Web Release Smoke Checks, Morley UI Consistency and Morley Ultimate Parity Gate passed. Repository Security Audit and Full Feature Contract Audit ended with GitHub Actions `startup_failure` before any job ran; GitHub rejected failed-job rerun because startup-failure runs have no retryable job. This ledger-only commit intentionally retriggers exact-head CI without changing production behavior.
