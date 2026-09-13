# Nova Next Functionality-First Design

## Goal
Turn the current Nova Next successor from a strong interactive shell into a genuinely usable daily-work app while preserving the approved layout, keeping current `nova/**` untouched, and maintaining the existing Admin-only and protected-action boundaries.

The first functionality slice promotes three currently static/staged areas—Tasks, Projects and Files—into real product surfaces. Automation, Calendar and Integrations become honest, useful read-only or staged surfaces only where safe existing services are already available. No protected Guardian, pricing, release, OTA, signing, role-management or destructive authority is added.

## Scope

### Tasks
Tasks become a real local-first workspace instead of hard-coded demo rows.

- Create, edit, complete/reopen and delete Nova Next tasks.
- Fields: title, notes, due date, priority, project link, completed state, created/updated timestamps.
- Filters: All, Today, Upcoming, Done.
- Persist in isolated storage key `nova-next.workspace.v1`; no current Nova data or other Morley storage keys are modified.
- Task writes remain device-local in this slice; no server mutation surface is introduced merely to make the UI interactive.
- Empty, loading, validation and storage-failure states are explicit.

### Projects
Projects become a real local-first organiser rather than fixed progress cards.

- Create and edit projects.
- Fields: name, description, status, optional target date, progress, created/updated timestamps.
- Project progress may be set directly and may also show task completion context when linked tasks exist.
- Project deletion requires explicit confirmation and only deletes the Nova Next local project record; linked tasks remain and have their project link cleared.
- Seed/demo project cards are not presented as live data once the runtime store is active.

### Files
Files becomes a usable session-oriented file workspace without inventing backend storage.

- Pick files from Android/browser file chooser.
- A Nova Next session may track at most 20 selected files.
- Files larger than 20 MiB are rejected before reading or handoff.
- Show file name, MIME/type, size and session-added time.
- Allow removing files from the current Nova Next session.
- File content remains local to the browser/WebView unless the user explicitly sends a supported file/image into an existing Nova capability.
- Image files can hand off to Nova Vision and are additionally subject to the existing Vision adapter image count/type/payload limits.
- UTF-8 text, Markdown, JSON and CSV files up to 1 MiB may be read locally and handed to guarded Chat only on explicit user action.
- Other supported metadata-only files remain selectable/listed but are never silently read or uploaded.
- Unsupported or oversized files are shown honestly; no fake upload/sync indicator.
- No persistent cloud file store is added in this slice.

### Automation
Automation becomes an honest capability-status page.

- Show supported Nova/Nova Next automation capabilities derived from existing safe runtime/registry data.
- Protected or unavailable workflow actions are marked read-only/staged.
- No new scheduling, GitHub mutation, release, Guardian repair or arbitrary action executor is introduced.

### Calendar
Calendar becomes a useful view over Nova Next local task/project dates.

- Show upcoming dated tasks and project target dates.
- No external calendar write integration is added in this slice.
- If no dated items exist, show a useful empty state with navigation back to Tasks/Projects.

### Integrations
Integrations becomes a truthful connection-status screen.

- Show only integrations the existing runtime can actually identify, such as authenticated Nova/Supabase and read-only GitHub broker status.
- No connector is shown as connected unless runtime evidence says it is.
- No credential entry or secret storage is added to the client.

## Architecture

### 1. Local workspace store
Add a small isolated module under `nova-next/src/` responsible only for Tasks and Projects persistence.

Responsibilities:
- schema/version validation;
- load/save using `nova-next.workspace.v1` only;
- immutable CRUD operations;
- date/priority/status validation;
- safe recovery from corrupt storage by clearing only the invalid Nova Next workspace payload, returning an empty valid workspace and surfacing a recovery notice to the UI.

The UI does not manipulate raw `localStorage` directly.

### 2. Workspace runtime
Add a runtime facade that exposes task/project operations and derived calendar data to the UI. It depends on the local workspace store only and has no privileged network authority.

### 3. File session service
Add a session-only service that validates the 20-file session cap, 20 MiB per-file cap, supported local-text types and 1 MiB text-read cap; tracks metadata/object references during the current app session; and routes supported images into the already-approved Vision adapter. It must never claim persistence or cloud upload.

### 4. Workspace UI controller
Keep `feature-ui.mjs` focused by moving Tasks, Projects, Files, Calendar, Automation and Integrations rendering/binding into a dedicated workspace UI module. Existing Knowledge, Vision, Code Proposal and Control Centre behavior remains separate.

### 5. Existing safe runtime reuse
Where network-backed status is useful, reuse the existing authenticated safe service facade. Do not widen `SAFE_CLIENT_FUNCTIONS` merely to fill a screen. If an existing endpoint mixes read and write actions, expose only the already-approved read adapter methods.

## Data Flow

Task/project action:
1. User interacts with Nova Next screen.
2. Workspace UI validates obvious form state.
3. Workspace runtime calls the local store.
4. Store validates full record, persists isolated state, returns canonical data.
5. UI re-renders from returned state.
6. Calendar derives its items from the same canonical workspace state.

File action:
1. User chooses files through browser/Android file picker.
2. File session service validates count, type and size.
3. Metadata appears in Files.
4. Supported image handoff calls the existing Vision runtime only on explicit user action.
5. Supported text handoff reads locally and prefills guarded Chat only on explicit user action.

Integration/status action:
1. Screen requests existing safe status methods.
2. Partial service failures render per-card unavailable states rather than collapsing the whole screen.
3. No unavailable capability is represented as connected or functional.

## UX and Interaction

The approved Nova Next visual language remains unchanged: dark navy/black surfaces, neon blue/purple accents, rounded glass-like cards and the current side drawer/bottom navigation.

New interactions use the existing bottom-sheet/modal language. Destructive local actions require confirmation. Forms keep primary actions obvious and disabled while submitting. Success feedback is short and non-blocking. Errors state what failed without fabricating data.

Tasks and Projects must feel native to the current shell rather than separate admin-style forms. Files should prioritise simple actions: Add files, Analyse image, Send text to Chat, Remove.

## Security and Governance Boundaries

- Current production `nova/**` remains untouched.
- Existing Admin authentication remains required before protected/network-backed Nova Next capabilities are used.
- Task/project local storage contains no authentication tokens or credentials.
- Files are session-local by default; no silent upload.
- No privileged backend/provider/signing credential is stored in client source.
- No Guardian repair execution or approval action.
- No pricing approval/write path.
- No user/role mutation.
- No deploy, release, OTA or signing action.
- No destructive backend action.
- Browser-side capability restrictions are not described as server authorization; server checks remain authoritative for network services.

## Error Handling

- Corrupt local workspace data: clear only `nova-next.workspace.v1`, present a recovery notice, and continue with an empty valid state.
- Storage unavailable/quota failure: do not report the operation as persisted; leave canonical persisted state unchanged and surface a clear save failure.
- File count/size/type violation: reject before reading or invoking any adapter and explain the exact limit.
- Vision/chat handoff failure: retain the file/list state and show retry-safe feedback.
- Safe status endpoint failure: show that integration/status card as unavailable while other cards continue to render.

## Testing

Use TDD for every new behavior.

Required contract coverage:
- task CRUD, validation, filtering and completion;
- project CRUD, confirmation-safe deletion behavior and task unlinking;
- exact isolated storage namespace and corrupt-data recovery;
- calendar derivation from task/project dates;
- 20-file session cap and 20 MiB per-file validation;
- 1 MiB text-read boundary plus supported text MIME/extensions;
- image-handoff delegation to existing Vision validation;
- no silent file upload/persistence claim;
- truthful integration status behavior under partial failure;
- UI contract tests for empty, populated, validation and failure states;
- existing 25+ Nova Next contracts remain green;
- JavaScript syntax remains green;
- Android wrapper unit/lint/debug APK build remains green through the isolated validator workflow when that workflow is approved/available.

## Acceptance Criteria

This slice is complete when:

1. Tasks no longer depend on hard-coded demo rows and all core local CRUD/filter interactions work after reload.
2. Projects no longer depend on hard-coded demo cards and core local CRUD/progress interactions work after reload.
3. Files can be selected and honestly managed within the session, with explicit supported handoffs to Vision/Chat and the exact limits above enforced.
4. Calendar reflects local dated Tasks/Projects rather than placeholder copy.
5. Automation and Integrations show truthful supported/staged/read-only states and never fake connectivity or authority.
6. Existing Chat, Auth, Vision, Knowledge, Code Proposal and Control Centre behavior does not regress.
7. Current Nova and protected Morley boundaries remain untouched.
8. The full Nova Next contract suite and repository gates pass before merge.

## Explicit Non-Goals for This Slice

- Cloud task/project sync.
- Persistent cloud file storage.
- External calendar writes.
- New third-party credential flows.
- Guardian repair execution.
- Pricing writes or approvals.
- Production Nova replacement or route cutover.
- Production Android package/signing migration.
- Release/OTA automation.

These can be designed as later protected slices once this workspace foundation is stable.