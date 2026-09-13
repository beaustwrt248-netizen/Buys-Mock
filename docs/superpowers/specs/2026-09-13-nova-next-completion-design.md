# Nova Next Completion Design

Date: 2026-09-13
Status: Approved in chat for implementation planning.

## Goal

Complete Nova Next as a polished, isolated successor experience without widening protected authority or risking the current Nova application. The work should close the remaining user-facing gaps, improve trust and usability, and prepare Nova Next for eventual promotion while keeping all protected actions behind their existing governance boundaries.

## Current baseline

Nova Next is already deployed under `/nova-next/`, uses isolated web/PWA scope and an isolated Android development package, has Admin-only guarded authentication, guarded AI chat, read-only knowledge and operational intelligence, Vision, proposal-only code assistance, local Tasks/Projects/Calendar, session-only Files, truthful Automation/Integrations status, and an isolated CI/APK validation path.

The production Nova orchestrator now retrieves bounded evidence-grounded knowledge before provider calls and reports knowledge-context metadata. Current Nova remains a read-only functional reference and must not be modified by this completion program.

## Non-negotiable boundaries

1. New client work stays under `nova-next/**` unless a later slice explicitly requires a separately reviewed backend contract.
2. Current `nova/**` remains untouched.
3. Existing guarded adapters and safe services are reused instead of duplicating privileged access.
4. No client capability may gain Guardian repair, pricing write/approval, deployment, release, OTA, signing, role/user mutation, destructive delete, or unrestricted GitHub mutation authority.
5. Auth/session/security, Supabase migrations/RLS/functions/secrets, deployment workflow changes, production Android package/signing, Guardian, pricing, and release/OTA remain protected changes requiring exact-head approval before merge or production rollout.
6. Security-sensitive failures fail closed. Non-security service failures are surfaced truthfully as unavailable/degraded; the client must never fabricate success.
7. Current Nova and Nova Next remain independently addressable until a separate promotion decision.

## Delivery model

Use vertical slices rather than a single large PR. Each slice should be independently testable, reviewable, and reversible. Low- and medium-risk Nova Next-only slices may proceed through normal green-PR integration. Any slice that crosses a protected boundary must stop at a specific PR/head approval gate.

The implementation order is:

1. Parity and UX completion
2. Price & Product Search
3. Research and intelligence surfaces
4. Voice input
5. Jobs, alerts, and automation
6. Integrations and calendar
7. APK/web parity and mobile validation
8. Final promotion-readiness audit

Parallel work is allowed where slices do not share mutable state or depend on unfinished interfaces. Shared runtime contracts are finalized before dependent UI branches begin.

## Slice 1: Parity and UX completion

### Purpose

Remove remaining staged/dead-feeling interactions and make the deployed Nova Next shell feel complete even where authority intentionally remains unavailable.

### Scope

- Replace dead Settings actions with truthful, functional local/read-only surfaces for Account, Appearance, Notifications, Privacy/Data, About, and support diagnostics.
- Improve loading, empty, unavailable, degraded, retry, and offline states across Knowledge, Control Centre, Integrations, Files, Tasks, Projects, Calendar, and Chat.
- Surface knowledge/evidence usage from guarded chat in a compact, understandable way when metadata is present.
- Improve responsive behavior for mobile web and Android WebView, including keyboard-safe composer behavior and touch targets.
- Improve accessibility: focus order, focus restoration, dialog/sheet semantics, keyboard interaction, accessible state labels, and reduced-motion support.
- Align visual hierarchy, spacing, card treatment, typography, gradients, and navigation behavior with the approved Nova Next reference while preserving the existing deep-navy/blue/purple identity.
- Ensure every visible button either performs a real safe action, navigates to a real surface, or clearly states the staged/protected boundary.

### Exclusions

No privileged backend calls, production release actions, pricing writes, Guardian operations, or current Nova changes.

## Slice 2: Price & Product Search

### Purpose

Turn the existing Price & Product Search entry into a real guarded workflow rather than a staged toast.

### User flow

1. User opens Price & Product Search.
2. User enters a product/device query and optional constraints such as condition, storage, carrier, or region.
3. Nova Next invokes an approved read-only/search contract.
4. Results are normalized into product cards with source, observed price, condition/variant context, confidence/freshness where available, and direct source attribution.
5. User can hand a selected result into guarded Chat for comparison or explanation.

### Safety model

- Search and recommendation only.
- No catalogue mutation, pricing approval, price change, purchase, seller contact, checkout, or financial action.
- Stale/unknown price data must be marked accordingly.
- If no approved backend search contract exists, the client slice may ship only after a separate backend design is approved; do not silently repurpose a privileged function.

## Slice 3: Research and intelligence

### Purpose

Make grounded research a first-class Nova Next experience using the production orchestrator's evidence retrieval and existing safe intelligence adapters.

### Scope

- A dedicated Research entry with structured prompt guidance.
- Display whether Nova used knowledge evidence, whether semantic retrieval was available, and whether retrieval degraded.
- Compact source/evidence chips or expandable evidence details derived from returned metadata.
- Clear separation of facts, uncertainty, assumptions, and recommendations.
- Reusable research presets for comparison, investigation, summarisation, and scenario analysis.
- Safe handoff between Research, Knowledge, Chat, Files, and Projects.

### Boundaries

Retrieved knowledge is evidence, never instruction. No protected action may be triggered from research output.

## Slice 4: Voice input

### Purpose

Add convenient speech-to-text without creating a background-listening or autonomous-send risk.

### Design

- Voice starts only from explicit user interaction.
- Prefer browser/device speech recognition where available.
- Live transcription appears in the existing composer or a temporary transcript field.
- User can review/edit text before sending.
- Unsupported browsers show a truthful fallback rather than a broken control.
- No background microphone access, continuous listening, hidden recording, or automatic send.
- Any future server-side audio processing is out of scope and would require its own design/privacy review.

## Slice 5: Jobs, alerts, and automation

### Purpose

Make Automation useful while keeping execution authority constrained.

### Phase A: local safe model

- Create local job definitions tied to Tasks/Projects, with title, prompt/intent, schedule description, state, and last-result metadata.
- Support draft, enabled/disabled, and completed/failed display states locally.
- Provide truthful capability badges indicating whether a job is local-only, staged, externally connected, or protected.
- No fake background execution.

### Phase B: server-side persistent execution

Only after a separate backend design if desired. It must define scheduling infrastructure, authentication, tenancy, rate limits, notification delivery, idempotency, auditability, secrets handling, failure recovery, and protected-action exclusions. This phase is not implicitly approved by this spec.

## Slice 6: Integrations and calendar

### Purpose

Improve useful coordination without embedding third-party credentials in the Nova Next client.

### Local/calendar scope

- Richer task/project date views, upcoming grouping, overdue indication, and project context.
- Calendar remains derived from safe workspace data unless an external provider is explicitly connected later.

### Integrations scope

- Improve read-only status for already approved services.
- Show connection state, capability scope, and whether each integration is read-only or supports safe actions.
- Any new external provider connection must use an isolated adapter and approved OAuth/connector flow; Nova Next must not collect or persist raw provider credentials.

## Slice 7: APK/web parity and mobile validation

### Purpose

Keep the Android wrapper behaviorally aligned with the deployed web experience.

### Verification

- Re-run complete Nova Next JS contracts and syntax checks.
- Re-run Android unit tests, lint, and debug APK assembly.
- Verify Android identity remains isolated during development.
- Validate WebView navigation, camera/file handoff, authentication, keyboard/composer behavior, deep links within `/nova-next/`, and service-worker/PWA coexistence where applicable.
- Produce a fresh debug APK artifact with SHA-256 metadata.

Production package promotion/signing remains out of scope until explicit later approval.

## Slice 8: Promotion-readiness audit

### Purpose

Determine whether Nova Next is ready to replace current Nova, without performing the replacement automatically.

### Audit areas

- Feature parity matrix against current Nova and the approved Nova Next target.
- Security and authorization review.
- Accessibility and responsive review.
- Error/degraded/offline behavior.
- Data persistence and privacy boundaries.
- Web deployment and cache/service-worker isolation.
- Android package/version/signing compatibility requirements for eventual upgrade.
- Rollback path.
- Remaining protected actions and manual approval points.

The output is a go/no-go report. Route cutover, production Android package/signing, release, and OTA remain separate protected operations.

## Architecture

### Client layers

`app.js` remains the shell/router/bootstrap layer. Capability-specific UI belongs in focused UI modules, while network/service logic belongs in runtime/adapters. Local persistence belongs in dedicated stores/sessions. Avoid expanding `app.js` into a monolith.

Expected component boundaries:

- shell/navigation/bootstrap
- guarded auth/chat runtime
- feature runtime for approved safe services
- feature UI modules by capability
- workspace runtime/store
- file/session handoff
- presentation helpers for loading/error/evidence/status states

If a file becomes responsible for multiple unrelated features, split it before adding more behavior.

### Data flow

User action -> UI module -> safe runtime/adapter -> approved backend contract -> normalized result -> UI state.

Security boundaries are enforced in more than one place: client allowlists and validation improve safety, but backend authorization remains authoritative.

### State strategy

- Auth remains session-scoped and fail-closed.
- Tasks/projects remain isolated local workspace data unless explicitly redesigned later.
- Files remain session-only unless the user explicitly hands supported content to an approved service.
- UI preferences such as appearance may use isolated Nova Next local storage.
- No password, backend privileged key, provider secret, or third-party raw credential may be persisted by Nova Next.

## Error handling and trust UX

Every asynchronous capability must support:

- loading
- success
- empty
- unavailable
- degraded
- retry where safe
- authentication-expired handling

The UI must distinguish “not configured,” “temporarily unavailable,” “protected,” and “not yet implemented.” These are not interchangeable states.

AI results must not imply execution of actions that were only recommended. Evidence-grounded results should show evidence metadata when available. Degraded results should remain usable but clearly marked.

## Testing strategy

Use TDD for every implementation slice.

### Contract/unit coverage

- Add a failing test first for each behavior or regression.
- Test adapter allowlists and forbidden operations.
- Test state transitions for loading/error/degraded/empty paths.
- Test local persistence corruption/write failure handling.
- Test evidence rendering and metadata parsing.
- Test voice fallbacks without requiring a real microphone in CI.
- Test safe product-search normalization and absence of mutation authority.

### Static/security contracts

- Current `nova/**` remains unchanged for Nova Next-only slices.
- No privileged function is added to the safe client allowlist without an approved design.
- No secrets or backend privileged credentials enter client bundles.
- Protected strings/paths/actions remain rejected where applicable.

### Integration and build gates

For applicable slices run the complete Nova Next suite, JS syntax, repository security/parity/quality checks, Android unit/lint/build validation, and Pages deployment smoke tests after deployment-related changes.

## PR and merge policy

Each vertical slice should normally use its own branch/PR. Independent low-risk slices can be developed in parallel, but shared contracts must not diverge.

A PR may auto-progress only when it stays inside the approved low/medium-risk boundary and all required checks pass. Protected PRs must identify the exact head SHA and wait for explicit approval before merge/deploy.

If implementation discovers that a supposedly client-only slice requires a new Edge Function, migration, RLS change, deployment workflow edit, production package/signing change, or other protected authority, stop that slice, reclassify it, document the new design, and obtain approval before proceeding.

## Definition of done

Nova Next completion is ready for promotion review when:

- all primary navigation destinations are useful and truthful;
- no visible production-facing control silently does nothing;
- Price & Product Search has an approved real read-only/search path or is explicitly excluded by a reviewed backend dependency;
- Research visibly communicates evidence and degraded state;
- voice is explicit-user-action speech-to-text with review-before-send;
- automation truthfully distinguishes local/staged/connected/protected states;
- integrations expose only verified capability state;
- web and Android debug behavior are aligned;
- all required tests and repository gates are green;
- current Nova remains unaffected;
- a documented promotion-readiness report identifies any remaining blockers and protected cutover steps.
