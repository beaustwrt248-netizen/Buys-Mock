# Nova Next Status

**Current audited state:** **GO FOR PROMOTION REVIEW**  
**Promotion-readiness report:** `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md`

Nova Next has completed the approved non-protected development program, including the responsive UI, launcher and isolated OTA implementation. It remains isolated from current production Nova until separately approved promotion actions are performed.

- Reference design and shared responsive shell: implemented from the approved Nova Next direction.
- Existing production Nova: remains independently rooted under `/nova/`; no production route replacement has been performed.
- Nova Next web scope: implemented independently under `/nova-next/` with safe-area handling and isolated service-worker/cache scope.
- Home, Chat, Tools, Tasks, Projects, Knowledge, Files, Automation, Calendar, Integrations, Help, Settings and Control Centre surfaces: implemented and regression-tested.
- Chat: persistent composer, one scrolling conversation viewport, empty-state quick actions and normal/error message states implemented.
- Tools: category + search filtering and valid destinations/actions implemented.
- Control Centre: explicit loading, healthy, degraded, unavailable and retry states implemented without granting protected operational authority.
- Auth/session: constrained client session handling and guarded adapters implemented; production Auth/RLS/provider-policy expansion remains protected.
- Vision/files, product search, research, voice and code proposals: implemented inside the existing safe-function/action-policy boundaries.
- Tasks/projects/calendar/preferences/automation metadata: implemented with namespaced constrained local persistence; session file contents are not persistently stored by the file-session layer.
- Accessibility: route focus, drawer keyboard behavior, password state labels, meaningful login-field names, polite loading status and reduced-motion behavior implemented and tested.
- Launcher identity: Nova orb web/PWA and Android adaptive launcher assets implemented.
- Android wrapper: isolated `com.buysloans.novanext` identity, tests, lint, debug build, checksum and artifact validation implemented.
- Native OTA: isolated Nova application/channel/package/version/hash validation and Android installer handoff implemented. Morley metadata/packages are rejected.
- PWA updates: service-worker update lifecycle is separate from native APK OTA and uses the Nova Next cache namespace.
- OTA publication: **manual and protected**. The signed `publish-nova-next-ota` job requires an explicit `workflow_dispatch`; merging or pushing to `main` does not publish an OTA release.
- Promotion readiness: no required non-protected capability is classified as blocked for promotion review.

## Protected promotion boundary

The **GO FOR PROMOTION REVIEW** verdict is not production-cutover or release authority. Production actions remain protected and were not performed by the completion merge. Separate explicit approval is still required for:

1. production web route replacement;
2. Android production application identity/signing/version compatibility changes;
3. signed release/OTA publication, metadata promotion or distribution;
4. backend authority expansion, including Auth/RLS/schema/functions/secrets, Guardian execution, pricing writes, role/user authority or destructive production-data access.

See `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md` and `docs/superpowers/plans/2026-09-15-nova-next-completion-ota.md` for the evidence, boundaries and implementation history.