# Morley AI

Morley AI is the single Admin intelligence layer. Guardian and Pricing Assistant become capabilities rather than independent authorities.

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

## Memory and learning

Morley AI separates authoritative facts from preferences/feedback. Admin approvals, rejections and corrections can improve future proposals, but feedback cannot rewrite security policy, approval requirements, audit rules or authoritative device facts.

Persisted memory should be structured, scoped, source-attributed, confidence-tagged and expiry-aware. Session memory in `admin/morley-ai-core.js` is deliberately bounded and non-authoritative.

## Confidence

Responses/proposals should identify whether evidence is verified, inferred, stale or unknown. Unknown device facts must be researched/verified rather than invented.

## Initial capabilities

1. Pricing: list/search price slots; preview natural-language pricing; protected application remains through `admin-pricing-control`.
2. Guardian: health/findings are readable; repair remains human-approved and retains Guardian protected boundaries.
3. Catalogue: read/search first; protected mutation added only through an audited server capability.

## Next capabilities

- cross-system diagnostics and root-cause correlation
- catalogue completeness/compatibility research queue
- valuation explanation and anomaly detection
- inventory/sales margin intelligence
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
