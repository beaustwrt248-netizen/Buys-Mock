# Morley AI Assessment Engine Design

## Purpose
Build one shared AI assessment engine for Morley Buys and Morley Admin that turns device evidence into an auditable assessment while preserving existing pricing, Guardian, auth/RLS and catalogue authority boundaries.

## Rollout
V1 is staff-first. The same assessment contracts are designed for later customer-facing intake. AI may collect evidence, identify devices, run or guide diagnostics, detect visible damage, calculate condition/valuation/repair recommendations, flag risk and prepare stock data. Staff must confirm final grade, buy price, repair disposition and stock/listing creation.

Low-risk actions may later graduate to automatic commit only after measured accuracy and override-rate thresholds are met. Security, permissions, pricing-policy changes and Guardian protected repair are never promoted by model confidence alone.

## Existing contracts to reuse
- `nova-intelligence-core.js` remains the evidence ranking/confidence primitive and is extended rather than replaced.
- `morley-central-pricing.js` / protected pricing APIs remain pricing authority; AI produces proposals only.
- Guardian remains observation/diagnosis plus protected repair enforcement; the assessment engine may emit diagnostics and consume verified outcomes but cannot bypass approval.
- Supabase migrations and Edge Functions remain the persistence/server execution pattern.
- Catalogue live-sync remains the device/specification source of truth. Unknown model/storage facts block commercial completion rather than being guessed.

## Architecture
`capture/evidence -> assessment orchestrator -> identity + damage + diagnostics -> condition score -> valuation/repair proposal -> risk checks -> staff confirmation -> device passport -> existing stock/listing workflows`

The assessment orchestrator is a shared domain service. UI surfaces consume its result; they do not each implement their own scoring or pricing logic.

## Core data model
### device_assessments
One row per assessment session. Stores catalogue/stock links, resolved model/storage, identity confidence, cosmetic score, functional score, Morley Condition Score, valuation confidence, state and model/rules versions.

### assessment_evidence
References intake images and structured observations. Stores source, evidence type, provenance, confidence, capture quality and verification status. Raw secrets and unrestricted IMEI/serial values are not exposed to general client reads.

### damage_findings
Per-image visible-damage findings with type, severity, confidence and normalized region geometry. Staff can accept, reject or amend a finding without deleting the original AI observation.

### diagnostic_results
Typed functional test results with `pass`, `fail`, `unknown` or `not_tested`, structured measurements, evidence source and confidence.

### valuation_quotes
Proposal records containing base market value, adjustments, estimated repair cost, target resale, proposed buy value, expected margin, confidence, explanation and pricing-rules version. Hard floors/ceilings and protected pricing policy are enforced outside the model.

### deal_risk_flags
Review-only flags for duplicate identifiers, evidence inconsistency, abnormal price, component/identity mismatch and other configured anomalies. A risk flag cannot automatically accuse or reject a customer.

### staff_overrides
Append-only record of confirmed/overridden grades, prices, repair decisions and reasons.

### device_passports / device_passport_events
Per-unit lifecycle record and append-only events: intake, evidence, diagnostics, valuation, override, repair, stock movement, markdown and sale.

### ai_decision_audit / ai_model_versions
Records model/rules versions, inputs by reference, outputs, confidence, actor, approval result and final outcome for calibration and regression analysis.

## Assessment state machine
`draft -> evidence_ready -> identified -> diagnostics_ready -> proposed -> review_required -> approved -> stock_prepared -> completed`

Blocking states include `identity_unresolved`, `storage_unresolved`, `evidence_insufficient` and `policy_blocked`. A blocked assessment can continue after new evidence or staff correction; it is not silently forced through.

## Camera and damage flow
V1 supports a guided minimum two-photo intake (front and rear) plus optional additional angles. Capture quality checks mark blur/glare/coverage issues and request a retake only when evidence is insufficient. Damage findings are returned as normalized rectangles/polygons so web and Android can render the same overlays.

The engine may infer candidate models but must resolve against the authoritative catalogue. Exact storage is evidence-gated; visual similarity alone cannot assign storage.

## Diagnostics and Morley Condition Score
Diagnostics combine platform-readable signals with guided staff tests. Unsupported/internal faults remain `unknown`, never fabricated.

Condition Score is deterministic and versioned in v1: cosmetic and functional sub-scores produce an overall 0-100 score plus a grade recommendation. Critical failures can cap the grade regardless of cosmetic score. The exact scoring weights live in versioned rules, not prompt text.

## Valuation and repair decision
Valuation consumes resolved device variant, condition, current protected price data, configured margin policy, market evidence already approved by Morley, historical outcomes where available and estimated repair cost.

Outputs are proposals with factor-level explanations. `Buy & repair`, `Buy as-is`, `Parts only` or `Decline/review` recommendations use expected resale, repair cost, margin and configured risk rules. V1 cannot commit the buy price or repair disposition without staff confirmation.

## Deal protection
Risk checks are deterministic where possible and model-assisted only for evidence comparison. High-severity flags force `review_required`; they never auto-reject. Full identifiers are protected by role/RLS and UI masking.

## One-photo-to-stock orchestration
The end-state workflow may prefill stock data, description, label metadata, catalogue link, proposed resale and publish payload. In v1 it stops at a review screen. The final stock/listing creation invokes existing protected workflows after staff confirmation.

## Nova integration
Nova receives permissioned read tools for assessment status, explanations, exceptions, calibration metrics and operational queries. Mutation tools call the same protected assessment/stock APIs and inherit their risk class; natural-language commands cannot lower approval requirements.

Example intents include: highest-confidence intake exceptions, assessments waiting on staff, likely repair opportunities, unusual valuation deviations, repeated diagnostic failures and catalogue facts blocking intake.

## Customer-facing reuse
A later Morley Buys customer intake uses the same assessment IDs and evidence contracts but a reduced permission surface. Customer sessions cannot see internal margins, risk heuristics, staff notes, protected identifiers or admin diagnostics.

## Error handling
Every stage returns structured status and recoverability. Network/model failures preserve gathered evidence and allow retry. Partial diagnostic coverage is explicit. Provider/model outages fall back to deterministic rules/manual selection rather than inventing outputs. Guardian receives sanitized operational failures without secrets or image contents.

## Security and privacy invariants
- No client-side service role.
- RLS/role checks protect assessment writes and internal commercial fields.
- IMEI/serial are masked for general UI and stored only through approved protected paths.
- AI cannot modify RLS, permissions, secrets, pricing policy or Guardian approval state.
- All consequential recommendations and overrides are attributable and auditable.
- Authoritative catalogue/pricing data overrides model memory.

## Testing
1. Unit tests for evidence gating, state transitions, deterministic condition score, valuation guardrails and risk rules.
2. Migration/RLS contract tests for staff/customer/admin visibility and write boundaries.
3. Integration tests proving unresolved identity/storage cannot reach approved/stock-prepared states.
4. Regression tests proving AI proposals cannot bypass pricing/Guardian protected actions.
5. Fixture-based damage geometry/diagnostic normalization tests independent of model provider.
6. Browser tests for staff review flow and recoverable failures.
7. Android parity tests for shared assessment contract and overlay coordinates.

## Telemetry and promotion criteria
Track assessment duration, identification accuracy, damage finding acceptance/rejection, diagnostic completion, grade override rate, price override rate, quote-to-buy conversion, valuation error vs realised outcome, repair recommendation profitability, risk flag precision and return rate.

Only explicitly selected low-risk actions may move from staff-confirmed to auto-commit, using documented thresholds and rollback. Commercial and security authority remains outside the model.

## Delivery slices
1. Assessment core: schema/contracts/state machine/audit + deterministic condition scoring.
2. Staff assessment client: Device Lens/Admin review surface and damage overlay contract.
3. Diagnostics + valuation/repair proposal integration.
4. Risk flags + Device Passport lifecycle.
5. Stock-preparation orchestration and protected confirmation.
6. Nova permissioned tools and operational queries.
7. Restricted customer-facing intake reuse.

Each slice must pass existing security, parity, feature-contract and quality gates before merge.

## Protected approval
On 2026-09-12, Beau explicitly approved the production assessment-schema migration, final merge, and Android release/version promotion. That approval is conditional on required CI, security, parity, signing, checksum and OTA monotonic-version gates remaining satisfied; it does not waive any of those controls.
