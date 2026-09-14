# Nova Next Feature Parity Map

Nova Next uses current Nova as the behavioural reference while preserving an independently implemented frontend/runtime and explicit authority boundaries. The detailed promotion-readiness evidence is recorded in `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md`.

Status meanings:

- `ready` — implementation, adapters/data path, expected failure states and regression evidence are connected for the audited Nova Next scope.
- `ready-with-boundary` — the audited capability is implemented, but protected production/write/execution authority deliberately remains outside Nova Next.

| Capability | Nova Next section | Audited status | Risk / boundary |
| --- | --- | --- | --- |
| Conversation / AI chat | Chat | ready | Live runtime and chat adapter implemented and tested. |
| Command discovery | Chat / Tools | ready | User-facing discovery/navigation implemented. |
| Multi-step jobs | Tasks / Automation | ready-with-boundary | Local automation/workspace runtime implemented; deploy/release/protected execution remains blocked. |
| Live web research | Tools / Knowledge | ready-with-boundary | Safe research/read path implemented; unknown/high-risk actions fail closed. |
| Camera / vision | Tools / Files | ready-with-boundary | User-initiated Vision adapter implemented with bounded session files; no privileged mutation authority. |
| Catalogue intelligence | Tools | ready-with-boundary | Safe catalogue/read capability implemented; patch application remains protected. |
| Pricing intelligence | Tools | ready-with-boundary | Product/pricing read/search implemented; pricing writes remain protected. |
| Support intelligence | Help / Tools | ready | Help/diagnostic capability implemented within read/support boundaries. |
| Business intelligence | Tools / Knowledge | ready-with-boundary | Read/analysis capability implemented; no production mutation authority added. |
| Scenario planning | Tools / Chat | ready | Planning interaction supported through the Nova Next runtime. |
| Voice assistant | Tools / Chat | ready-with-boundary | Voice input/runtime implemented; action policy still governs execution. |
| Memory | Knowledge / Workspace | ready-with-boundary | Namespaced constrained persistence implemented; file contents remain session-memory only. |
| Learning controls | Knowledge | ready-with-boundary | Knowledge/learning surfaces and safe adapter boundary implemented; protected policy changes remain outside scope. |
| Evidence review | Knowledge / Control Centre | ready-with-boundary | Read/evidence surfaces implemented without repair/write authority. |
| Explain mode | Knowledge / Chat | ready | Explanation interaction supported through the audited runtime. |
| Guardian analysis | Control Centre | ready-with-boundary | Read/evidence boundary exists; Guardian repair approval/execution is explicitly protected and client-blocked. |
| Release readiness | Control Centre | ready-with-boundary | Readiness/status evidence implemented; deploy/signing/release/OTA remain protected. |
| Bug triage | Control Centre / Help | ready-with-boundary | Diagnostic/triage surfaces implemented without autonomous repair authority. |
| Proactive alerts | Automation / Control Centre | ready-with-boundary | Local automation metadata/status implemented; protected external execution remains gated. |
| Tasks | Tasks | ready | Validated lifecycle and namespaced local persistence implemented. |
| Projects | Projects | ready | Validated project lifecycle and task unlinking implemented. |
| Calendar | Calendar | ready-with-boundary | Workspace-derived dates implemented; no external calendar-write authority is granted. |
| Integrations | Integrations | ready-with-boundary | Verified status/capability surface implemented without credential-entry/storage authority. |
| Settings / accessibility | Settings / shell | ready | Preferences, diagnostics and verified accessibility contracts implemented. |
| Android wrapper | Android | ready-with-boundary | Isolated development identity/build validated; production package/signing/version remains protected. |

## Promotion boundary

The audited state is **GO FOR PROMOTION REVIEW**. This does not authorize production web route replacement, Android production application identity/signing/version compatibility changes, release/OTA, or backend authority expansion. Those remain separate protected approval gates.
