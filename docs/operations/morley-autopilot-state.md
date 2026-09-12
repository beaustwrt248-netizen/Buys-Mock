# Morley Ecosystem Autopilot State

Last reconciled: 2026-09-12 22:12 AWST
Source of truth: live GitHub, Supabase and connected-service evidence. Gumtree remains excluded unless Beau explicitly re-enables it.

## Current repository state

- `main`: `48fc591a2eafede8488f4934681568d49d48de0c` — approved PR #1696 merged; routine auto-merge arming is now best-effort while least-privilege workflow permissions remain intact.
- Active protected draft PR: #1701 `Restore full Morley AI, Search and Device Lens intelligence` on `design/morley-restore-ai-search`.
- #1701 remains draft/incomplete and must not merge until all applicable functional, security, parity, visual/accessibility and release checks are satisfied and Beau explicitly approves protected changes.
- Morley Buys advertised OTA release remains 2.15.100 / versionCode 144 with APK SHA-256 `488cf42be87f54201266f9ea5e109911365677524e1f043beabcbf32cd16f175`.

## Active workstream: Morley AI / Device Lens restoration (#1701)

### Completed and verified on branch

- Repair-or-Buy deterministic policy is implemented in the shared assessment core.
- Required commercial inputs: buy cost, as-is resale, after-repair resale, repair cost and minimum margin. Missing inputs fail closed to staff review; explicitly unverified inputs fail closed to staff review.
- Supported advisory outcomes: `buy_as_is`, `buy_and_repair`, `parts_only`, `review_required`.
- Tie behavior is deterministic and prefers the lower-intervention as-is route when margins are equal.
- Parts-only requires verified parts economics and must strictly beat the selected alternative while clearing minimum margin.
- All outputs remain advisory and require staff confirmation.
- Durable proposal persistence is implemented through existing schema contracts: `valuation_quotes`, pending `ai_decision_audit`, Device Passport `repair` event, and assessment proposal state. The final commercial `repair_decision` is not written by AI proposal persistence.
- TDD evidence: new Repair-or-Buy tests were observed failing before implementation, then passing after implementation; client persistence tests were observed failing with `persistRepairProposal is not a function`, then passing after the client implementation.
- Fresh green evidence on the completed Task-6 head includes Morley AI Assessment Core, client tests, valuation/repair tests and Repository Security Audit.

### In progress

- Android Device Lens assessment-review integration is now in RED-first TDD.
- New contract test `MorleyAssessmentUiContractTest.kt` requires: explicit unresolved identity/storage states, `not_tested` hardware state, Condition/Valuation/Repair-or-Buy/Risk review sections, explicit staff confirmation before commercial action, and Morley-blue Material styling.
- Production implementation has not yet been added for that test; Android CI is currently evaluating the RED commit.

## Production-first triage

### Release / CI

- Current release identity remains 2.15.100 / versionCode 144.
- #1701 changes Android code while still carrying the live 144 / 2.15.100 identity. Ultimate Parity therefore correctly fails release monotonicity for this unfinished draft branch. Do not bump release metadata merely to silence CI; versioning/OTA promotion remains protected and should happen only for a completed release candidate.
- UI checklist and mobile-web-theme checklist gates remain unsatisfied until the actual visual/accessibility review is completed. Do not add completion markers early.
- Restore Point Capture previously exposed shallow-history behavior on older branch state; any further workflow-security changes remain protected and approval-gated.

### Auth / Supabase security

- Issue #1686 remains open for Supabase leaked-password protection. This Auth setting is protected and was not changed automatically.
- Existing RLS/security boundaries remain authoritative. No new schema/RLS/role changes were introduced by the Repair-or-Buy persistence work; it uses existing assessment tables/contracts.

### Guardian / Nova

- Guardian code-changing repair remains human-approval gated.
- Nova provider/model routing should not be tuned from absent or insufficient production telemetry. Continue non-sensitive evaluation fixtures before changing routing/cost defaults.

### Catalogue / data integrity

- Preserve manufacturer-first verification and Australian variants. Never guess model numbers, release dates, RAM/storage/SIM/chipset/specifications.
- Duplicate/conflicting identifiers remain triage signals, not authorization to merge/delete production records.

### Backup / recovery

- Global Google Drive backup remains fail-closed because the OAuth refresh credential is expired/revoked.
- Recovery is not healthy until an authorized fresh credential produces a new Drive upload and successful digest read-back.

## Impact / risk queue

Scoring: impact/confidence 1-5 higher is better; risk/effort/dependency risk 1-5 higher means more caution/cost. Priority never authorizes protected work.

| Workstream | Impact | Risk | Effort | Confidence | Dependency risk | Next safe action |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| #1701 Device Lens assessment integration | 5 | 3 | 3 | 5 | 4 | Complete Android RED->GREEN review UI integration; keep business actions staff-confirmed. |
| #1701 Admin assessment workspace | 5 | 3 | 4 | 4 | 4 | After Android contract is green, add searchable/filterable assessment review with explicit confirmation controls. |
| #1701 universal buy search | 5 | 3 | 4 | 4 | 4 | Resume existing branch tests/implementation after assessment review path is stable. |
| Global backup recovery | 5 | 5 | 2 | 5 | 5 | Keep fail-closed; after authorized credential replacement, prove fresh upload + digest read-back. |
| Auth leaked-password protection #1686 | 5 | 5 | 1 | 5 | 5 | Await explicit approval; change only the protected Auth setting, then rerun advisor and auth regression. |
| Catalogue verification | 5 | 2 | 5 | 5 | 3 | Continue manufacturer-first verification without guessing/destructive reconciliation. |
| Nova evaluation / multi-model routing | 4 | 3 | 3 | 4 | 4 | Collect non-sensitive accuracy/latency/cost/fallback evidence before changing provider defaults. |
| Guardian | 4 | 4 | 2 | 5 | 4 | Diagnose incidents; prepare repair PRs only; retain human approval for code changes. |
| Release integrity | 5 | 5 | 2 | 5 | 5 | Keep 2.15.100 immutable until #1701 or another candidate is complete and independently validated. |

## Dependency / contract map

- **Morley Buys Android** -> auth/session -> Device Lens/photos -> assessment evidence -> condition/diagnostics -> live pricing -> valuation/Repair-or-Buy/risk -> explicit staff confirmation -> stock preparation -> signed APK/OTA.
- **Website** -> catalogue/pricing contracts -> auth/session -> Nova/valuation/search -> deployment integrity.
- **Admin** -> auth/role boundaries -> assessment review -> support/governance -> catalogue controls -> Admin release.
- **Nova** -> verified evidence/catalogue -> assessment tools -> provider routing -> evaluation/telemetry -> protected action boundaries.
- **Guardian** -> diagnostics + repository context + Nova analysis -> human approval for code-changing repair.
- **Supabase** -> Auth + RLS/RPC authorization -> assessment tables/passports/audit -> catalogue/Realtime -> backup metadata.
- **Release** -> monotonic version -> quality/security/parity -> signed artifact/checksum -> immutable release -> protected OTA metadata.
- **Backup** -> scheduler -> Edge Function -> Google OAuth -> Drive upload -> digest read-back -> retention/audit.

## Critical invariants

- Never invent device facts; unresolved identity/storage must stay unresolved.
- Hardware tests must be real observations or `not_tested`; never infer a pass.
- AI commercial output is advisory only; staff explicitly confirms final grade, buy price, repair decision and stock publication.
- Raw IMEI/serial/secret data must not leak into ordinary assessment history, metadata, logs or AI audit payloads.
- App/web/Admin/Nova must share catalogue and assessment contracts without silent divergence.
- Duplicate catalogue identifiers/groups are triage signals only.
- Inventory lifecycle and stock identifiers must remain logically valid.
- Auth/session changes must preserve caller verification and role checks.
- OTA versionCode/versionName/tag/APK URL/SHA-256 must describe the same signed bytes; published releases are immutable.
- Backup health requires fresh integrity/read-back evidence, not scheduler success.

## Failure-pattern knowledge

- **Routine review succeeds, merge arming fails:** integration cannot invoke `mergePullRequest`; #1696 made arming best-effort without expanding permissions.
- **Privileged workflow shell interpolation:** #1678 moved PR-controlled values to inert env transport and retained security regression coverage.
- **Device Lens two-photo timeout:** #1669 / 2.15.100 added bounded image/provider/client windows and retryable timeout handling.
- **Release-parity failure on feature branch:** branch APK identity equals live release. Treat as release-readiness signal; do not bump version early.
- **Backup scheduler can appear healthy while Drive write fails:** require verified upload + digest read-back.

## Protected boundaries

Auth/authorization/RLS, secrets/credentials, destructive schema/data, privileged roles, protected pricing policy, Guardian repair authority, GitHub workflow/repository security, release signing/checksum and OTA/deployment promotion require explicit approval. Same-owner actions are never independent approval.

## Next safe actions

1. Complete Device Lens assessment review RED->GREEN on #1701 without changing protected release/Auth/RLS boundaries.
2. Continue #1701 Admin assessment review workspace after Android review contracts are green.
3. Resume universal buy-search implementation/tests already present on #1701; avoid duplicate branches.
4. Keep #1686 Auth hardening and Google OAuth credential replacement approval-gated.
5. Preserve live 2.15.100 release identity until a complete monotonic release candidate exists.
6. Continue manufacturer-first catalogue verification and Nova evaluation evidence gathering in independent lanes when CI is running.