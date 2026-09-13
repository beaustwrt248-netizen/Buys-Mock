# Nova Next Feature Parity Map

Nova Next uses current Nova as a behavioural reference while keeping a new frontend/runtime implementation. A capability is not considered migrated until the real adapter/data source, authorization, evidence/observability, failure states and regression coverage are connected.

| Capability | Nova Next section | Bootstrap status | Risk |
| --- | --- | --- | --- |
| Conversation / AI chat | Chat | Registry + UI shell | Medium |
| Command discovery | Chat | Registry | Low |
| Multi-step jobs | Tasks / Automation | Registry | Medium |
| Live web research | Tools | Registry | Medium |
| Camera / vision | Tools | Registry | Medium |
| Catalogue intelligence | Tools | Registry | Medium |
| Pricing intelligence | Tools | Registry; protected writes blocked | High |
| Support intelligence | Tools | Registry | Medium |
| Business intelligence | Tools | Registry | Medium |
| Scenario planning | Tools | Registry | Low |
| Voice assistant | Tools / Chat | Registry | Medium |
| Memory | Knowledge | Registry | Medium |
| Learning controls | Knowledge | Registry | High |
| Evidence review | Knowledge | Registry | Medium |
| Explain mode | Knowledge | Registry | Low |
| Guardian analysis | More / Control Centre | Registry; protected actions blocked | High |
| Release readiness | More / Control Centre | Registry; deploy/OTA blocked | High |
| Bug triage | More / Control Centre | Registry | Medium |
| Proactive alerts | More / Control Centre | Registry | Medium |

## Bootstrap rule

`Registry + UI shell` means the destination and contract exist; it is not a claim that the live backend feature is connected. Live parity is promoted individually only after adapter, permissions, evidence and failure-state tests pass.
