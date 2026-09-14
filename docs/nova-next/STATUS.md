# Nova Next Status

**Current audited state:** **GO FOR PROMOTION REVIEW**  
**Promotion-readiness report:** `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md`

Nova Next has completed the approved non-protected development program and remains isolated from current production Nova until a separately approved promotion is performed.

- Reference design: implemented from the approved Nova Next design direction.
- Existing production Nova: remains independently rooted under `/nova/`; no route replacement has been performed.
- Nova Next web scope: implemented and deployed independently under `/nova-next/`.
- Shell/navigation: splash, login, home, chat, tools, tasks, projects, settings, knowledge, files, automation, calendar, integrations, help and control-centre surfaces implemented.
- Auth: client/auth adapters and constrained session handling implemented and tested; production Auth/RLS/provider-policy expansion remains protected.
- Chat/live runtime: implemented and regression-tested.
- Knowledge/research: implemented through safe read/research adapters with fail-closed protected-action boundaries.
- Vision/files: user-initiated Vision flow implemented; session files remain bounded and in-memory unless explicitly sent through a supported action.
- Code proposals: implemented as proposal-only capability; deploy/apply/Guardian/signing authority is not implied.
- Tasks/projects/calendar: workspace lifecycle and derived calendar behavior implemented with namespaced local persistence.
- Product search/pricing intelligence: read/search capability implemented; protected pricing writes remain blocked.
- Voice: input/runtime UI implemented and covered by the Nova Next contract suite.
- Automation: local job metadata/runtime surfaces implemented; deploy, release/OTA, Guardian repair, pricing write, role/user and destructive authority remain blocked.
- Integrations: verified status/capability surfaces implemented without credential-entry/storage authority on the screen.
- Help/settings/accessibility: implemented, including route focus handling, drawer keyboard behavior, password-state labels, meaningful login-field names, polite loading status and reduced-motion support.
- PWA/service worker: Nova Next cache/path namespace isolation implemented; reduced-motion stylesheet is included in the offline core cache.
- Android wrapper: isolated development identity `com.buysloans.novanext` implemented and validated through unit tests, lint, debug assembly, checksum verification and artifact upload.
- Deployment evidence: merged functional main passed the Pages deployment and post-deploy smoke workflow for both current `/nova/` and isolated `/nova-next/` resources.
- Promotion readiness: audited with no required capability classified as blocked for promotion review.

## Protected promotion boundary

The **GO FOR PROMOTION REVIEW** verdict is not production-cutover authority. The following remain separate approval gates:

1. production web route replacement;
2. Android production application identity/signing/version compatibility;
3. release/OTA publishing or distribution;
4. backend authority expansion, including Auth/RLS/schema/functions/secrets, Guardian execution, pricing writes, role/user authority or destructive production-data access.

See `docs/superpowers/reports/2026-09-13-nova-next-promotion-readiness.md` for the immutable evidence, capability classifications, deployment/cache/mobile findings and exact protected next actions.
