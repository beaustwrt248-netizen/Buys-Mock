# Morley AI

Morley AI is the single Admin intelligence layer. Guardian and Pricing Assistant are capabilities rather than independent intelligence authorities, while their protected execution controls remain separate enforcement boundaries.

## Design

`conversation -> planner -> capability registry -> approval boundary -> existing protected API/Edge Function -> audit`

The AI may have broad read knowledge but never receives an unrestricted database/service-role escape hatch.

## Knowledge domains

- device catalogue and Australian compatibility policy
- buy pricing and price history
- valuations and valuation evidence
- inventory, sales and realised margin
- support and diagnostics
- releases, app health and Guardian findings
- users/admin governance (metadata only according to role)
- repository/release evidence where exposed through approved server tools

## Risk classes

- `read`: automatic, non-mutating retrieval
- `low`: reversible/non-sensitive operations explicitly registered as safe
- `protected`: pricing, catalogue, support/admin state and other consequential writes; explicit approval required
- `critical`: releases, account/permission/security changes and Guardian repair execution; explicit approval plus existing subsystem protections required

No capability may downgrade the protection already enforced by its backing subsystem.

## Guardian relationship

Guardian is part of Morley's operating brain for observation, diagnosis, recurrence detection and learning from verified repair outcomes. Guardian remains the protected repair execution and approval subsystem. Morley can understand what Guardian knows and use that history to improve future diagnosis, but Morley cannot grant itself permission to execute a protected repair or bypass Guardian's approval state.

## Memory and deeper learning

Morley AI separates authoritative facts from preferences/feedback. Admin approvals, rejections and corrections can improve future proposals, but feedback cannot rewrite security policy, approval requirements, audit rules or authoritative device facts.

Morley may synthesise operational experience from existing audited source tables instead of creating an AI-owned shadow system of record. Current learning signals include verified Guardian incident outcomes, recurring classifications/routes, repair completion/failure outcomes, realised valuation profit, valuation forecast error and forecast bias. These signals improve context, prioritisation and recommendations only.

Persisted memory should be structured, scoped, source-attributed, confidence-tagged and expiry-aware. Session memory in `admin/morley-ai-core.js` is deliberately bounded and non-authoritative. Authoritative live sources always override learned/session memory.

Learning can never lower a capability's risk class, bypass approval, grant authority, modify RLS/permissions/secrets, or make a protected action autonomous.

## Confidence

Responses/proposals should identify whether evidence is verified, inferred, stale or unknown. Unknown device facts must be researched/verified rather than invented.

## Connected capabilities

1. Pricing: list/search price slots; preview natural-language pricing; protected application remains through `admin-pricing-control`.
2. Guardian: health/findings/diagnoses and historical outcomes are readable; repair remains human-approved and retains Guardian protected boundaries.
3. Catalogue: live health and search are readable; protected mutation is not introduced here.
4. Valuations: recent performance and realised-profit signals are readable; protected mutation is not introduced here.
5. Learning: Morley derives recurrence, repair-success and valuation-calibration signals from authoritative historical outcomes.

## Next capabilities

- inventory/sales margin intelligence
- cross-system diagnostics and root-cause correlation
- catalogue completeness/compatibility research queue
- valuation anomaly detection and calibration by category/grade
- support-ticket triage and suggested replies
- release readiness and regression evidence summaries
- proactive operational briefings and alerts
- semantic knowledge retrieval over approved Morley documentation and decisions

## Non-negotiable invariants

- no unrestricted client-side service role
- no silent protected writes
- no autonomous security/permission changes
- no autonomous Guardian protected repair
- every consequential write remains attributable and auditable
- source-of-truth data beats model memory
