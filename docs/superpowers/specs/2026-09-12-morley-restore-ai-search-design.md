# Morley Restore, AI Scan History, Repair-or-Buy and Search Modes Design

## Purpose
Make major Morley changes recoverable by default, preserve every AI scan as durable operational evidence, expose the existing Repair-or-Buy intelligence as a staff workflow, and give Search Bar, Manual Search, Price Check and AI Scan distinct jobs while retaining Universal Buy Search as the shared engine.

## Scope
Applies to Morley Buys Android, Morley Buys web/mobile web, Morley Admin web/native surfaces, Nova, Guardian, Supabase functions/schema, release metadata and deployment workflows in this repository.

## 1. Backup & Restore Center
A major change must create a restore point before production deployment. A restore point is immutable metadata describing the known-good state immediately before the change: source commit, affected components, Android/Admin versions where applicable, release/OTA identity, web deployment/source revision, Supabase migration head, relevant Edge Function versions, configuration fingerprints that contain no secrets, artifact references/checksums, creation actor/time, verification state and change/PR reference.

Restore points do not copy secrets into the database or repository. Existing immutable Git history, signed release assets and provider deployment/version history remain the payload sources; the Restore Center indexes and verifies them.

### Restore safety
Rollback is component-scoped. Restoring application/web/function code must not automatically roll back newer business data. Database rollback is a separate protected operation and must use an explicitly authored forward-safe compensating migration or approved recovery procedure. No generic destructive database rewind button is allowed.

Restore actions are preview-first. The UI shows current state, target state, components affected, data/schema compatibility, blockers and required approvals. Protected rollback actions require explicit authorised confirmation and are audited. Guardian/Nova may diagnose or prepare a rollback plan but cannot bypass approval boundaries.

### Lifecycle
`captured -> deployment_pending -> deployed -> verified` or `deployment_failed`. A restore operation creates a new audit event and never mutates the historical restore point.

### UI
Morley Admin gains `Backups & Restore` with restore-point history, status, affected components, release/source identifiers, health verification and component-level restore/prepare actions. Other apps expose recovery status only where useful; privileged rollback controls remain in Admin.

## 2. Persistent AI Scan History
Every Device Lens AI scan creates a `device_assessments` session at scan start, before the first photo. The assessment ID becomes the durable scan ID. Failed, cancelled and incomplete scans are retained rather than silently deleted.

Photos are stored through protected Supabase Storage paths and referenced from `assessment_evidence`; local temporary files remain disposable caches. Scan state is checkpointed after front capture, rear capture, analysis result/failure, damage review, pricing, repair recommendation, staff confirmation and stock conversion.

AI Scan History lists completed, incomplete, failed and cancelled scans with model/variant when known, created time, current state, confidence and outcome. Staff can reopen, resume, retry analysis, review evidence, repeat pricing and continue eligible assessments into stock. Deleting scan history is not part of this change; retention is permanent unless a separately governed retention policy is introduced.

Network failure must not discard a scan. Android keeps a minimal pending checkpoint locally and retries server persistence when connectivity returns. Raw IMEI/serial values remain excluded from general scan-history metadata.

## 3. Repair-or-Buy Workflow
Reuse the existing versioned `MorleyAssessmentCore.decideRepairStrategy()` rules rather than introducing a second decision engine. The staff UI combines verified device identity/storage, condition/damage, diagnostics, proposed buy cost, resale as-is, estimated repair cost, resale after repair, expected margins and expected repair/sell-through timing.

Recommendations are `Buy & repair`, `Buy as-is`, `Parts only`, or `Review / decline`. The existing rules are extended to represent parts-only explicitly when a verified parts recovery value clears configured policy while normal resale options do not. Recommendations remain advisory and require staff confirmation before commercial or stock actions.

The recommendation and subsequent staff decision are persisted to the assessment and Device Passport. Later realised repair cost, sale value and time-to-sale can be compared with the recommendation through existing audit/passport structures without granting the model authority to change pricing policy.

## 4. Universal Buy Search Modes
Universal Buy Search remains the shared matching/catalogue/pricing engine, but entry points become explicit modes instead of duplicate navigation aliases.

- `quick_search`: dashboard/search bar; instant text/code suggestions and fastest path to an item.
- `manual_search`: guided category -> brand -> model -> storage/variant selection, with deliberate correction controls.
- `price_check`: identify/select an item then move directly to condition and valuation evidence/results.
- `ai_scan`: camera-first Device Lens flow -> identification -> damage/condition -> valuation -> Repair-or-Buy.

All modes share catalogue matching, protected pricing authority and result models. UI mode controls orchestration/presentation only; it must not fork pricing logic. Deep links/intents preserve mode so Android and web/mobile web behave consistently.

## Data and interfaces
Extend existing assessment persistence rather than creating customer profiles or a parallel inventory-intelligence system. Add restore-point/audit schema separately from assessment schema. New tables/functions use existing Admin/Manager/support role helpers and RLS conventions.

Key interfaces:
- `createAssessment({ state: 'capture_started', source: 'device_lens' }) -> assessmentId`
- `checkpointAssessment(assessmentId, checkpoint)`
- `listScanHistory(filters)` / `getScanHistory(assessmentId)`
- `decideRepairStrategy(input) -> advisory recommendation`
- `confirmCommercialDecision(assessmentId, { repairDecision, explicitStaffConfirmation: true })`
- `createRestorePoint(manifest) -> restorePointId`
- `previewRestore(restorePointId, components) -> compatibility/blockers`
- protected `executeRestore(...)` only after authorised approval.

## Error handling
Persistence is fail-safe: capture can continue with a local pending checkpoint during temporary network failure, but commercial completion cannot claim durable audit success until the server assessment is synced. Provider failures preserve evidence and produce retryable scan states. Restore preview blocks incompatible schema/code combinations rather than guessing. Health-check failure after deployment marks the restore point/deployment failed and surfaces the previous verified restore target.

## Security invariants
- No client-side service role or secret snapshots.
- Existing Auth/RLS, Guardian, pricing and release approval boundaries remain intact.
- Restore metadata contains references/checksums, not credentials.
- Production-data rollback is never automatic.
- AI recommendations never auto-commit buy price, repair disposition, stock publication or protected rollback.
- Raw protected device identifiers remain masked/referenced through approved paths.
- Every consequential restore/commercial decision is attributable and auditable.

## Testing
Use TDD for each delivery slice. Add contract tests for immutable restore manifests, no-secret fields, component-scoped rollback and protected database recovery; assessment tests proving a scan ID exists before first capture and failed/cancelled scans persist; offline checkpoint/retry tests; Repair-or-Buy fixtures including parts-only and staff-confirmation gates; Universal Search mode routing/parity tests; Android unit/lint and web/Admin regression coverage; Supabase migration/RLS tests; existing security, feature-contract, quality, distribution and parity gates.

## Delivery slices
1. Backup/Restore foundation and Admin read/preview surface.
2. Persistent AI Scan History and evidence checkpointing.
3. Repair-or-Buy UI/persistence and parts-only rule.
4. Universal Buy Search modes and entry-point navigation cleanup.
5. Cross-app parity, release health verification and protected production rollout.

Each slice creates/uses a restore point before its own production deployment. Database migrations are forward-only unless a separately reviewed compensating migration is required.

## Approval
Beau approved this architecture in chat on 2026-09-12. Implementation remains subject to existing CI, security, RLS, Guardian, signing, release/OTA and protected-action approval gates; this design does not waive them.