# Nova Next Promotion Readiness Report

**Audit date:** 2026-09-15 (Australia/Perth)  
**Audited merged main:** `a4c7344756edd7b07ccbcfae88418876f043003e`  
**Decision:** **GO FOR PROMOTION REVIEW**

This is a readiness decision only. It does **not** authorize or perform the protected production cutover, Android production identity/signing/version changes, release/OTA, backend authority expansion, Guardian repair execution, pricing writes, destructive actions, or production-data changes.

## Evidence baseline

The final functional candidate immediately before merge was `77c549e9846d94b9c2591a038ecf820a2bb1ab16` from PR #2199. Its Nova Next isolated validator completed the full Node contract suite, JavaScript syntax checks, isolated Android identity check, Android unit tests, lint, debug APK assembly, checksum verification, and artifact upload successfully in workflow run `34870784672`. The repository security audit, quality gate, route/password accessibility gate, Ultimate Parity gate, path-stability contract, recovery contract, pricing-supersession contract, catalogue classification contract, and email contract were also green on that exact candidate.

PR #2199 was squash-merged to `main` as `a4c7344756edd7b07ccbcfae88418876f043003e`. The resulting main push ran `Deploy B&L Morley Web` workflow run `34871084215` successfully. That run completed release readiness audit, Nova Next Pages deployment contract, web and Nova Next syntax checks, static bundle construction, Pages deployment, and the post-deploy smoke-test step. The deployment workflow copies both `nova/` and `nova-next/` into independent paths and its post-deploy smoke contract verifies both current Nova and Nova Next resources. Repository `CNAME` is `buyshub.me`.

The isolated debug APK produced from the final functional candidate is `Nova-Next-0.1.0-dev-debug.apk`, 6,400,312 bytes. Its independently rechecked SHA-256 is:

`e1be15c2cb11163306afe53b87b8faecd7f2a5799b4c1b05a2cb9661deba1650`

## Feature parity matrix

Status meanings: **ready** = verified implementation and regression evidence; **ready-with-boundary** = verified implementation whose deliberately limited authority must remain intact; **blocked** = non-protected requirement prevents promotion review; **not-applicable** = intentionally outside Nova Next.

| Capability | Status | Evidence and boundary |
| --- | --- | --- |
| Auth | ready-with-boundary | Supabase/client auth adapters and constrained session persistence are covered by the Nova Next suite. Session storage contains only the access/refresh-token session shape and user metadata. Auth/RLS/provider policy expansion remains outside this audit. |
| Chat | ready | Live runtime and chat adapter are wired and covered by the complete Nova Next contract suite. Deployment smoke verifies the deployed Nova Next application module contains the live runtime. |
| Knowledge/research | ready-with-boundary | Knowledge/research UI and safe-service adapters are present and tested. Web research is classed medium-risk and auto-permitted, while unknown/high-risk actions fail closed. Evidence/read paths do not grant repair or write authority. |
| Vision | ready-with-boundary | `nova-vision` is client-allowlisted as an approved safe function. File/image submission is user initiated and constrained by the file-session type/size limits. |
| Code proposals | ready-with-boundary | Code proposal adapter is present and tested. Proposal generation does not imply deploy, catalogue-patch apply, Guardian execution, signing, or destructive authority; those actions are protected by policy. |
| Files | ready-with-boundary | File contents are held in the in-memory session file store, capped at 20 files / 20 MiB each with supported type checks. Files are only transformed for Chat/Vision when explicitly selected; they are not persisted by the file-session store. |
| Tasks | ready | Workspace store provides validated create/update/toggle/delete behavior using namespaced `nova-next.workspace.v1` local persistence and contract tests. |
| Projects | ready | Workspace store provides validated project lifecycle behavior, including task unlinking when a project is deleted, under the same namespaced local persistence contract. |
| Calendar | ready-with-boundary | Calendar derives workspace dates within Nova Next. No external calendar credential storage or autonomous calendar-write authority is introduced by the audited build. |
| Product search | ready-with-boundary | Approved read/search clients include `market-search-v2` and `app-pricing-catalogue`; protected pricing writes remain blocked and require separate authority. |
| Voice | ready-with-boundary | Voice input/runtime UI is present and covered by the Nova Next contract suite. It feeds user-facing interaction and does not bypass the action-policy boundary for protected execution. |
| Automation | ready-with-boundary | Automation metadata is namespaced under `nova-next.automation.v1` with validated lifecycle state. The UI/runtime does not gain release, OTA, Guardian-repair, pricing-write, role/user-change, destructive-delete, or deployment authority. |
| Integrations | ready-with-boundary | Integration status/capability surfaces are implemented without credential-entry/storage authority on the screen. Connected actions remain subject to safe-function/action policy boundaries. |
| Help/settings | ready | Help/settings surfaces, local appearance/notification preferences under `nova-next.preferences.v1`, accessibility behavior, and diagnostics are implemented and covered by contracts. |
| Android wrapper | ready-with-boundary | Debug wrapper uses namespace/application ID `com.buysloans.novanext`, debug suffix `.debug`, version `0.1.0-dev`, and `NOVA_NEXT_URL=https://buyshub.me/nova-next/`. Unit, lint, assemble, identity, checksum, and artifact-upload checks passed. Production package identity/signing/version compatibility remains protected. |

No capability in the required matrix is `blocked` for promotion review. Several are intentionally `ready-with-boundary` because promotion must not silently expand authority.

## Security, privacy and accessibility

### Client authority and fail-closed policy

`nova-next/src/safe-client-functions.mjs` maintains an explicit safe-function allowlist and an explicit blocked set. Guardian repair/status workers that can mutate, admin pricing control, and admin user control are not client-safe execution paths. `nova-next/src/action-policy.mjs` marks pricing writes, Guardian repair approval/execution, deploy, OTA publish, signing changes, role/user changes, destructive deletion and catalogue patch application as high-risk, protected, and non-auto-executable. Unknown actions are also treated as high-risk/protected/non-auto-executable.

Repository Security Audit was green on the final functional candidate. No evidence from the audited client files or contracts indicates privileged server credentials are embedded in the Nova Next client bundle. This report does not broaden the safe-function list or action policy.

### Persistence boundaries

- Auth session: namespaced Nova Next session storage with a constrained token/user session shape; malformed state is cleared.
- Files: in-memory session `Map`; no file-content persistence in the file-session store.
- Workspace: `nova-next.workspace.v1` local storage for tasks/projects with schema validation and invalid-state recovery/reset.
- Preferences: `nova-next.preferences.v1` local storage containing appearance and notification preference only.
- Automation: `nova-next.automation.v1` local storage containing local job metadata/state; this does not itself grant remote execution authority.

### Accessibility and responsive evidence

The final candidate includes and tests:

- route-heading focus handoff and route accessibility behavior;
- drawer keyboard containment and focus restoration;
- password visibility button label/state updates;
- meaningful accessible names for login identity/password inputs;
- polite live/status semantics for splash loading state;
- reduced-motion support through `prefers-reduced-motion: reduce`;
- offline precaching of the reduced-motion stylesheet;
- existing responsive/mobile contracts and Android WebView wrapper validation.

`Nova Next Route Password Accessibility` and `Nova Next Isolated Validation` passed on the exact final functional candidate. Android lint and tests passed before artifact staging. No protected production mobile identity was used.

## Deployment, cache and mobile evidence

### Web deployment

Merged main `a4c7344756edd7b07ccbcfae88418876f043003e` triggered `Deploy B&L Morley Web` run `34871084215`, which completed successfully. Its post-deploy smoke tests completed successfully after Pages deployment. The deployment workflow explicitly:

- copies current `nova/` to the static bundle;
- copies `nova-next/` independently to the static bundle;
- validates required files for both applications before upload;
- verifies deployed `/nova/`, `/nova/app.js` and `/nova/app-core.js` content;
- verifies deployed `/nova-next/` contains `<title>Nova Next</title>`;
- verifies deployed `/nova-next/app.js` contains `createLiveRuntime` and passes JavaScript syntax checking.

The configured Pages custom domain is `buyshub.me`, and the Android development wrapper targets `https://buyshub.me/nova-next/`. Current Nova remains independently rooted at `/nova/`; no route replacement was performed.

### Cache and service-worker isolation

`nova-next/service-worker.js` uses cache name `nova-next-shell-v3`, derives `APP_PREFIX` from the service worker's own directory, constructs all core paths beneath that prefix, restricts fetch interception to same-origin paths in the Nova Next `CORE` set, and only deletes caches whose names start with `nova-next-`. This prevents the Nova Next worker from claiming the production Nova `/nova/` asset namespace through its own static-path contract.

`nova-next/src/promotion-config.mjs` separately records development scope `/nova-next/` with app ID `com.buysloans.novanext` and production scope `/nova/` with app ID `com.buysloans.nova`; production explicitly requires promotion.

### Android evidence

Final functional candidate workflow run `34870784672` passed:

1. complete Nova Next contract suite;
2. JavaScript syntax verification;
3. isolated Android identity verification;
4. `:app:testDebugUnitTest`;
5. `:app:lintDebug`;
6. `:app:assembleDebug`;
7. APK staging and SHA-256 verification;
8. artifact upload.

Artifact: `Nova-Next-0.1.0-dev-debug.apk`  
Size: `6,400,312` bytes  
SHA-256: `e1be15c2cb11163306afe53b87b8faecd7f2a5799b4c1b05a2cb9661deba1650`

The debug build is evidence for readiness, not a production release artifact.

## Decision

**GO FOR PROMOTION REVIEW**

All non-protected completion requirements in the approved Nova Next completion plan have current evidence: required capability groups are implemented with explicit boundaries, the final candidate passed Nova Next/security/quality/parity validation, the debug Android wrapper passed unit/lint/build/identity/checksum checks, the merged main deployment and post-deploy web smoke checks succeeded, current Nova remains independently deployed, and Nova Next cache/package/web scopes remain isolated.

This decision means the project may proceed to a separate, explicit human review of the protected promotion steps. It is **not** approval to execute those steps. The present development lane remains safe to merge because it only adds audit documentation and its regression contract.

### Follow-up classification

- **Low/medium follow-up:** continue ordinary accessibility, browser/device coverage and non-authority UX refinement as defects are discovered; none currently blocks promotion review.
- **Protected dependency:** production web route/package/signing/release/backend-authority decisions below require explicit approval and their own rollback/verification plan.

## Protected next actions

The following actions were deliberately **not** performed by this audit and must remain separate approval gates:

1. **production web route replacement** — replacing or redirecting the current `/nova/` production route to Nova Next;
2. **Android production application identity/signing/version compatibility** — adopting `com.buysloans.nova`, production signing material, version/upgrade compatibility, store/release identity or equivalent production package changes;
3. **release/OTA** — publishing, promoting, signing, distributing, forcing or scheduling a production release/update;
4. **backend authority expansion** — changing Auth/RLS, database/schema, privileged edge functions, secrets/providers, Guardian execution authority, pricing-write authority, role/user authority or destructive production-data access.

Before any protected promotion, record fresh rollback artifacts, confirm production signing/version compatibility, repeat the live smoke/security/parity gates against the exact promotion candidate, and obtain explicit human approval for the exact action set.
