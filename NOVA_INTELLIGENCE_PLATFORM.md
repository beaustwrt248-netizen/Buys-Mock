# Nova Intelligence Platform

Nova is the cross-app operator for Morley. This specification extends `NOVA_AUTONOMOUS_DEVELOPMENT.md`; its approval boundaries remain authoritative.

## Delivery priority

1. Live web intelligence.
2. Deep catalogue agent.
3. Release + Guardian intelligence.
4. Camera/image intelligence.
5. Pricing intelligence.
6. Support intelligence.
7. Business intelligence, memory, voice, staff modes, scenario planning and broader task execution.

Capabilities may share infrastructure, but protected production actions must never be bundled into autonomous approval.

## 1. Live web intelligence

Nova must use real, fresh web retrieval rather than model memory for research claims that can change. Every externally-derived claim stores and displays source URL, source title/publisher, retrieval time, evidence excerpt or structured field, freshness, and confidence.

### Australian source order

For device identity/specification research, rank evidence in this order unless the requested fact requires a different primary authority:

1. Australian manufacturer product/support/manual/regulatory source.
2. Global manufacturer product/support/manual/regulatory source.
3. Australian carrier source (Telstra, Optus, Vodafone and relevant MVNO records).
4. Australian direct retailer source (for example JB Hi-Fi, Officeworks, Apple AU, Big W and other legitimate direct sellers).
5. Reputable Australian secondary/historical source.
6. Global reputable secondary source only when Australian evidence is unavailable.

Never infer a model number, storage option, release year or Australian variant from a reseller-generated title alone. Conflicting sources must be shown as a conflict, not silently resolved. Exact hardware revisions use the revision/model's release information, not the family/generation launch year.

Nova research responses expose: `answer`, `evidence[]`, `confidence`, `freshness`, `catalogueComparison`, and `recommendedAction`.

## 2. Deep catalogue agent

Nova continuously audits the live Morley catalogue and creates findings for:

- missing Australian-market devices;
- missing or suspect model numbers;
- wrong or incomplete storage;
- duplicate/near-duplicate records;
- incorrect release years, including revision-year errors;
- reseller-generated or malformed names;
- incomplete or contradictory specifications;
- category errors;
- image gaps/broken images;
- records that are 3G-only or otherwise fail current Australian catalogue rules.

Each finding contains current value, proposed value, field-level evidence, source priority, confidence, risk, dedupe key and a reversible proposed patch. Findings are queued for review. Nova may prepare clean corrections but does not silently mutate protected catalogue state.

Catalogue counts are computed live from the current data source after each search/audit; Nova must not report a cached or hard-coded total as current.

## 3. Pricing intelligence

Compare Morley stock against eligible evidence from eBay, Gumtree, Facebook Marketplace, Cash Converters and direct Australian retailers. Reddit, generic aggregators, SEO pages, forums and unrelated results are excluded from pricing comparables.

Normalise device/model/storage/condition, listing type, seller type, location, postage and freshness before comparison. Explain sample size, median/range, excluded outliers, confidence and why a price is under/over market. Pricing recommendations never bypass existing pricing approval logic.

## 4. Image and camera intelligence

Nova accepts staff-captured or uploaded images and can suggest likely family/model, colour, visible condition defects, missing parts/accessories, readable storage/model labels and serial/model stickers. It compares the candidate against catalogue records and lists missing verification fields.

Image-derived identity is probabilistic. Serial/IMEI or other sensitive identifiers must be access-controlled, minimally retained and never exposed in general logs. High-impact trade-in decisions require staff confirmation.

## 5. Support agent

Nova reads authorised support data, clusters likely duplicates, detects urgency/SLA risk, drafts replies, links probable known defects and suggests escalation. Drafting and classification can be autonomous; sending externally or making protected account/data changes follows role and approval policy.

## 6. Guardian analyst

Guardian intelligence is evidence-first. Nova explains the triggering signal, relevant logs, repeated patterns, correlated deployment/commit/workflow changes, confidence, blast radius and safest repair path. It can prepare diagnostics, regression tests, branches and PRs but must preserve Guardian human-approval and protected-repair boundaries.

## 7. Release manager

Nova monitors GitHub checks/workflows, pending PRs, release artifacts, web/app parity, Android/APK version state, OTA state when available, deployment drift and release blockers. A release brief distinguishes observed state from inference and links every blocker to evidence.

Release/deployment/OTA controls remain high-risk. Nova can prepare but cannot approve or bypass them.

## 8. Autonomous bug triage

From screenshots, crash logs, diagnostics and support reports Nova creates a reproducible incident record, maps symptoms to likely source files, ranks hypotheses, proposes the smallest patch, adds regression coverage and prepares a PR. It must not claim reproduction unless reproduction actually occurred.

## 9. Business dashboard intelligence

Provide conversational briefs for sales, margin, valuations, inventory movement, ageing stock, category performance, slow sellers, support load and operational risk. Metrics include data timestamp, period, comparison period and provenance. No fabricated metric when a source is unavailable.

## 10. Useful memory

Store durable project decisions and outcome feedback as scoped, auditable knowledge: catalogue rules, Australian sourcing rules, UI standards, rejected approaches, previous fixes and approval/rejection rationale. Memory never silently overrides protected policy. Rule changes require explicit provenance and the applicable approval.

## 11. Voice mode

Voice is an input/output surface over the same permissioned Nova command system. Spoken commands create the same auditable intents as typed commands. Before a protected write, Nova presents the action and waits for the same approval required in text mode.

## 12. Staff assistant mode

Every Nova request carries an authenticated role and permission context. Admin, manager and staff views filter data, actions and recommendations server-side; hiding a control in the UI is not authorization.

## 13. Task execution

Nova may safely perform authorised reads and reversible preparation work: draft support responses, prepare catalogue corrections, generate reports, file issues, create branches, implement bounded patches and prepare PRs. Protected production actions remain gated by `NOVA_AUTONOMOUS_DEVELOPMENT.md`.

All actions produce an audit event with actor, role, intent, inputs, evidence, proposed/actual changes, approval state and outcome.

## 14. Proactive alerts

Alerts are generated from live measurements and deduplicated by fingerprint. Initial alert classes include catalogue field regressions, unexpected catalogue-count drops, support SLA risk, release/check failures, deployment drift and pricing drift. Alerts include severity, first/last observed time, evidence and recommended next action.

## 15. Explain mode

Every material recommendation can expose `why`, evidence, confidence, source freshness, assumptions, conflicts and data used. Confidence must decrease when identity matching or source agreement is weak.

## 16. Scenario planning

Scenarios are explicitly simulations. They show baseline, changed assumptions, method, projected effect/range and uncertainty. A scenario never changes production pricing or stock by itself.

## 17. Natural multi-step commands

Commands such as “Check all Motorola devices, verify missing specs against Australian sources, ignore 3G-only models, and prepare fixes” are represented as a durable job with steps, checkpoints, evidence and resumable state. Nova carries the job through until complete, blocked, or approval is required rather than collapsing it into a single chat answer.

## 18. Learning from outcomes

Approvals/rejections and their reasons feed a recommendation-quality history. Learning may tune ranking/confidence and suggest future defaults, but cannot silently rewrite protected rules, permissions, approval boundaries or source policy.

## 19. Cross-app control centre

The Nova app is the central operator across Morley website, Morley Admin, catalogue, Guardian, support, GitHub/releases and future tools. Integrations expose narrow permissioned adapters; Nova does not require embedding a separate unrestricted AI agent in each product.

## Shared data contracts

Minimum logical records:

- `nova_jobs`: intent, scope, status, role, created/updated timestamps, checkpoint, blocker.
- `nova_evidence`: job/finding id, URL/source, publisher, source class, retrieved_at, freshness, excerpt/field, confidence.
- `nova_findings`: domain, entity id, field, current/proposed values, severity, confidence, dedupe key, state.
- `nova_actions`: requested action, risk class, reversible flag, approval requirement/state, result.
- `nova_alerts`: fingerprint, class, severity, first/last seen, evidence refs, acknowledgement state.
- `nova_outcomes`: recommendation/action id, approved/rejected/result, reason, feedback tags.
- `nova_memory`: scoped rule/decision, provenance, status, supersedes id, protected flag.

Database/RLS implementation is intentionally not applied by this specification because production schema/security changes are high-risk and require explicit approval.

## Updates navigation

The standalone **Updates** destination is removed from primary navigation. **Account** becomes the parent destination for Updates. The Account screen contains an **Updates** section/card with current app/web version, available update/release state, release notes/history and update actions allowed for the signed-in role. Deep links to the old Updates destination should redirect/open `Account > Updates` so existing links do not dead-end.

## Definition of done

A capability is not “live” merely because a prompt mentions it. It is live only when its required adapter/data source is connected, permissions are enforced, evidence is observable in the UI/API, failure states are handled, regression coverage exists and the relevant repository checks pass.
