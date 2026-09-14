# Nova Next Feature Parity Map

Nova Next uses current Nova as the behavioural reference while preserving an independently implemented frontend/runtime and explicit authority boundaries. Detailed promotion-readiness evidence is recorded in `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md`.

Status meanings:

- `ready` — implementation, expected failure states and regression evidence are connected for the audited Nova Next scope.
- `ready-with-boundary` — the capability is implemented, but protected production/write/release/execution authority deliberately remains outside Nova Next.

| Capability | Nova Next section | Audited status | Boundary |
| --- | --- | --- | --- |
| Conversation / AI chat | Chat | ready | Live runtime, scrolling conversation, persistent composer and guarded failure states implemented. |
| Command discovery | Chat / Tools | ready | User-facing discovery, filtering, search and navigation implemented. |
| Multi-step jobs | Tasks / Automation | ready-with-boundary | Local workspace/automation lifecycle implemented; protected external execution remains gated. |
| Live web research | Tools / Knowledge | ready-with-boundary | Safe research/read path implemented; unknown/high-risk actions fail closed. |
| Camera / vision | Tools / Files | ready-with-boundary | User-initiated Vision path implemented with bounded session files; no privileged mutation authority. |
| Catalogue intelligence | Tools | ready-with-boundary | Safe catalogue/read capability implemented; catalogue patch application remains protected. |
| Pricing intelligence | Tools | ready-with-boundary | Product/pricing read/search implemented; pricing writes remain protected. |
| Support intelligence | Help / Tools | ready | Help and diagnostic capability implemented inside support/read boundaries. |
| Business intelligence | Tools / Knowledge | ready-with-boundary | Read/analysis capability implemented without production mutation authority. |
| Scenario planning | Tools / Chat | ready | Planning interaction supported through the Nova Next runtime. |
| Voice assistant | Tools / Chat | ready-with-boundary | Voice input/runtime implemented; action policy continues to govern execution. |
| Memory | Knowledge / Workspace | ready-with-boundary | Namespaced constrained persistence implemented; session file contents remain non-persistent. |
| Learning controls | Knowledge | ready-with-boundary | Knowledge/learning surfaces and safe adapter boundary implemented; protected policy changes remain outside scope. |
| Evidence review | Knowledge / Control Centre | ready-with-boundary | Evidence/read surfaces implemented without repair/write authority. |
| Explain mode | Knowledge / Chat | ready | Explanation interaction supported through the audited runtime. |
| Guardian analysis | Control Centre | ready-with-boundary | Read/evidence boundary exists; Guardian repair approval/execution remains protected and client-blocked. |
| Release readiness | Control Centre | ready-with-boundary | Readiness/status evidence and isolated update machinery implemented; production promotion/signing/release remain protected. |
| Bug triage | Control Centre / Help | ready-with-boundary | Diagnostic/triage states implemented without autonomous repair authority. |
| Proactive alerts | Automation / Control Centre | ready-with-boundary | Local automation metadata/status implemented; protected external execution remains gated. |
| Tasks | Tasks | ready | Validated lifecycle and namespaced local persistence implemented. |
| Projects | Projects | ready | Validated project lifecycle and task unlinking implemented. |
| Calendar | Calendar | ready-with-boundary | Workspace-derived dates implemented; no external calendar-write authority is granted. |
| Integrations | Integrations | ready-with-boundary | Verified status/capability surfaces implemented without credential-entry/storage authority. |
| Settings / accessibility | Settings / shell | ready | Preferences, diagnostics, responsive shell and accessibility contracts implemented. |
| PWA / launcher identity | Shell / manifest | ready | Standard/maskable Nova orb identity and isolated web update lifecycle implemented. |
| Android wrapper | Android | ready-with-boundary | Isolated development package, adaptive icon, tests/lint/build and update manager validated; production package/signing remains protected. |
| Native OTA | Android / release workflow | ready-with-boundary | Nova-only metadata/package/hash/version validation and installer handoff implemented. OTA publication is **manual and protected**; only explicit workflow dispatch can publish a signed Nova release. |

## Promotion boundary

The audited state is **GO FOR PROMOTION REVIEW**. This does not authorize production web route replacement, Android production application identity/signing/version changes, signed release/OTA publication or backend authority expansion. Current production Nova remains authoritative until those separately protected actions are explicitly approved.