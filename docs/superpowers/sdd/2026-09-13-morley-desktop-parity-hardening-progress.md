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
