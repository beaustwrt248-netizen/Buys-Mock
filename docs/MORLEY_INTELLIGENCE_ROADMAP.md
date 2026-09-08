# Morley Buys Intelligence Platform Roadmap

Status: implementation roadmap

## Product boundary

Morley Buys remains focused on staff tooling, catalogue/device intelligence, pricing, marketplace operations, support, system safety and automation.

Explicitly excluded:
- Customer profiles / CRM
- Inventory intelligence / inventory lifecycle management

Existing permissions, human approval boundaries and Guardian protected operations must remain authoritative.

## 1. Morley Command Centre

Create a unified operational home surface using existing evidence-backed data sources. Surface Nova attention items, catalogue quality, pricing opportunities, marketplace reconciliation, support pressure, release/deployment health and Guardian evidence. Do not invent counts when a source is unavailable; display unavailable/unknown states explicitly.

## 2. Nova Actions

Extend Nova from read-only intelligence to permission-aware actions. Actions must be classified as read-only, reversible, approval-required or protected. Protected/destructive actions remain human gated. Every executed action must retain actor, evidence, proposed change, result and timestamp sufficient for audit/recovery.

Initial actions:
- propose catalogue corrections
- queue catalogue research
- propose pricing changes
- trigger safe refresh/reconciliation jobs
- prepare marketplace corrections
- route support/Guardian attention

## 3. Advanced Pricing Engine

Add an evidence-first Australian pricing layer supporting manufacturer-first product identity, direct Australian retailers and approved marketplace sources. Exclude Reddit and low-quality aggregator/search-result evidence from price recommendations.

Outputs:
- observed market range
- recommended buy price
- recommended sell price
- expected gross margin
- confidence/evidence freshness
- source provenance
- pricing history

Never silently substitute missing market evidence with fabricated values.

## 4. Sell-Speed Intelligence

Estimate relative demand and sell-speed from available historical/market evidence. Keep predictions explicitly probabilistic and expose confidence. Use sell-speed as one input to pricing recommendations rather than an automatic price authority.

## 5. AI Device Identification

Provide an image-assisted workflow that can propose brand, family/model, colour, visible model labels and visible condition indicators. Staff confirmation is required before proposed identification changes authoritative catalogue/device data. Never infer storage/IMEI/model number when it is not visible or otherwise evidenced.

## 6. Device Testing Assistant

Add guided test sessions for supported device capabilities such as display/touch, cameras, speaker, microphone, charging, buttons, vibration and connectivity. Record unsupported/unavailable checks distinctly from pass/fail. Avoid claiming automated hardware verification where the platform cannot perform it.

## 7. Catalogue Autopilot

Continuously detect:
- missing images
- missing/questionable model numbers
- missing storage variants
- incorrect release years
- duplicate/reseller-generated names
- incomplete specifications
- Australian/global variant conflicts

Source priority:
1. Australian manufacturer
2. global manufacturer/support/manual/regulatory archives
3. Australian carrier/retailer
4. reputable secondary historical sources

Autopilot proposes or queues evidence-backed fixes; uncertain identity/specification changes require review.

## 8. Marketplace Manager

Create a reconciliation surface for approved marketplaces, initially Gumtree with extensible adapters for eBay/Facebook Marketplace. Match listings using stock/item identifiers from title or description plus existing listing URLs where available.

Surface:
- matched
- missing
- stale
- duplicate
- price mismatch
- uncertain match

Deletion remains an explicit destructive action and must not be inferred from reconciliation alone.

## 9. Nova + Guardian Integration

Nova owns intelligence, explanation, orchestration and permitted actions. Guardian remains the safety/enforcement layer for protected operations and evidence. Nova must not bypass Guardian approval or repair boundaries. Present one unified Needs Attention experience without conflating their authorities.

## 10. Business Analytics

Add non-CRM, non-inventory-lifecycle analytics around pricing, catalogue coverage, category/device trends, marketplace quality, support pressure, Nova effectiveness and system/release health. Metrics must retain provenance and explicit unavailable states.

## 11. Smart Alerts

Evidence-backed alerts for meaningful changes such as:
- material market price movement
- new/missing device evidence
- catalogue conflict
- marketplace mismatch/stale listing
- failed deployment/workflow
- Guardian escalation
- support SLA pressure

Deduplicate repeated alerts and clear stale alerts when authoritative evidence resolves.

## 12. Integrations

Strengthen existing GitHub and Google Drive/backup integrations and create adapter boundaries for retailer/manufacturer research and marketplace reconciliation. Secrets must remain server-side and integrations must fail closed where authorization/evidence is unavailable.

## 13. Device Intelligence Centre

A canonical per-device research surface showing Australian model identity, model numbers, storage/colour variants, release data, specifications, images, evidence provenance, market observations, pricing history and Nova research/conflicts. Prefer authoritative Australian evidence and clearly distinguish confirmed facts from proposals.

## Delivery order

1. Shared evidence/action contracts and permission boundaries
2. Command Centre shell using existing live sources
3. Device Intelligence Centre
4. Catalogue Autopilot findings/queues
5. Pricing Engine + sell-speed evidence model
6. Marketplace Manager reconciliation
7. Nova Actions through Guardian-aware action contracts
8. Smart Alerts
9. Device Identification + Testing Assistant
10. Analytics and integration hardening

Each slice requires mobile/desktop regression coverage, permission/security checks, truthfulness/no-fabricated-evidence checks and existing quality gates before merge.
