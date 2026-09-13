# Nova Next Parity Wave 2 Design

## Goal
Finish the remaining low/medium-risk Nova Next parity gaps without changing the current `nova/**` application or widening protected Guardian, pricing-write, deployment, signing, OTA, release, user-role, or credential authority.

## Scope
This wave delivers seven safe vertical slices:

1. UX/state polish: truthful account/appearance/notification settings, consistent loading/error/retry states, responsive navigation polish, and explicit evidence/degraded indicators.
2. Product & price search: authenticated read-only catalogue/market search through existing backend read endpoints; no pricing mutation or approval actions.
3. Research/evidence: dedicated research composer/presentation using existing guarded Nova orchestration and evidence-aware responses; no new privileged backend action.
4. Voice input: browser/device speech recognition that only fills the chat composer. The user reviews text before sending; no background microphone use.
5. Local jobs & alerts: device-local definitions/status only. No server scheduler, push worker, background executor, email sender, or protected automation mutation is introduced.
6. Integrations/calendar: clearer read-only connection state and local calendar/job visibility using existing status adapters and local workspace data.
7. Android/web parity: keep `com.buysloans.novanext`, rebuild/lint/test the isolated wrapper, and preserve the `/nova-next/` URL.

## Safety boundaries
- Current `nova/**` remains untouched.
- Client runtime may call only explicitly allowlisted read/advisory Edge Functions.
- `admin-pricing-control`, Guardian repair, release/OTA, signing, role/user changes, and protected workflow mutations remain unavailable.
- Product search may use `app-pricing-catalogue` and/or `market-search-v2` only after server-side authentication validation is confirmed. No write endpoint is added.
- Voice recognition starts only from an explicit button press, stops on completion/error/cancel, and never sends automatically.
- Local jobs/alerts are stored only under a Nova Next namespaced key and do not claim server execution.
- Settings must never imply persistence or connection that does not exist.
- Fail closed for auth/security; render truthful degraded/unavailable states for read-only features.

## Product search design
Nova Next receives a dedicated read-only product search adapter. Catalogue search filters returned device/pricing data locally after a single authenticated catalogue fetch and supports brand/model/model-number/storage queries. Market search invokes the existing authenticated `market-search-v2` endpoint and renders used-marketplace vs retail-reference results distinctly. Result links are opened only by explicit user action. No automatic price update, recommendation write, or catalogue mutation occurs.

## Voice design
A small `voice-input.mjs` adapter wraps `SpeechRecognition`/`webkitSpeechRecognition` when available. It exposes supported/start/stop state and transcript callbacks. The UI inserts transcript text into `#novaNextChatInput` and does not submit the chat form.

## Local jobs/alerts design
A `local-jobs.mjs` store persists only JSON metadata under `nova-next.jobs.v1`: id, title, kind, condition/cadence text, enabled flag, created/updated timestamps. UI copy explicitly states that these are local definitions and do not run in the background. This creates a truthful foundation for later governed server scheduling without simulating execution.

## UX/settings design
- Account: show signed-in email/session status from the existing runtime; no profile editing unless a safe existing adapter exists.
- Appearance: local theme preference (`system|dark|light`) under `nova-next.appearance.v1`; only Nova Next UI is affected.
- Notifications: local preference explaining that push/background delivery is not yet connected; no permission request unless an actual delivery path exists.
- Retry buttons are shown for recoverable read-only loads.
- Research and chat results visually distinguish guarded/degraded/evidence state when response metadata is available.

## Acceptance criteria
- All new behavior has RED-first contract tests.
- Existing Nova Next tests remain green.
- Android lint/unit/debug APK build remains green.
- No changed path under `nova/**` or protected Supabase mutation/Guardian/release code.
- Product/market search is read-only and authenticated.
- Voice never sends automatically.
- Jobs/alerts never claim background execution.
- Settings copy is truthful and persistent only where explicitly implemented.
