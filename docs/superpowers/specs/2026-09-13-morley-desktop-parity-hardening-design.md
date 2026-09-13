# Morley Desktop Parity Hardening Design

## Goal

Make the rebuilt Morley Buys desktop shell behave as a reliable production workspace across every visible navigation item and quick action, while preserving mobile behavior, existing pricing/authentication business logic, Guardian boundaries, and the current deployment contract.

## Scope

This pass covers the rebuilt >=1000px desktop experience introduced by PR #1826. It audits and hardens:

- Dashboard
- Search & Scan
- Devices
- Catalogue
- Stock
- Sales
- Trade In / Buy
- Price Check
- AI Insights
- Reports
- Settings
- Scan Device shortcut
- Global search
- Notifications
- Account/profile controls
- Help/support entry points
- Desktop/tablet transition behavior at the 1000px boundary

Mobile/physical-phone presentation remains authoritative below 1000px and is not redesigned in this pass.

## Current risks identified

The rebuilt shell is visually complete and deployed, but several controls currently use broad route fallbacks rather than explicit capability ownership. Examples include AI Insights reusing Home, Reports reusing Sales, Search & Scan routing only to Universal Search, profile/account having no dedicated click behavior, notification count being static, several category cards reusing unrelated routes, and dashboard charts/category bars being decorative rather than clearly labeled snapshots. These are parity and trust risks even when the underlying business workflows themselves are healthy.

## Design principles

1. **Reuse existing business logic.** Desktop controls should hand off to established Morley pages/functions rather than duplicate pricing, inventory, sales, authentication, catalogue, scanner, notification, or support logic.
2. **Never fake capability.** A visible desktop item must either open its real workflow, clearly expose an intentional composite/fallback, or be removed/relabeled. No dead controls and no misleading static badges.
3. **Preserve runtime state.** The existing park/restore approach for legacy Home DOM nodes remains the model for responsive transitions so listeners and state survive desktop/mobile boundary changes.
4. **Explicit capability adapters.** Add a small desktop capability resolver that chooses the best existing route/function for each shell action and centralizes graceful fallback behavior.
5. **Progressive enhancement only.** If a specialized runtime function is absent, the shell falls back to a known existing page without throwing.
6. **Live data where available.** Notification badge, account label, KPI summaries, category/activity summaries, and report export should source existing runtime/DOM/storage state. Decorative visualizations must be marked as summaries or generated from real values.
7. **Accessible interaction.** Every icon-only or ambiguous desktop control gets a usable label, keyboard/focus behavior, and no hidden unreachable state when the sidebar is collapsed.

## Architecture

### Desktop capability adapter

`morley-desktop-rebuild.js` will define a centralized capability map for shell features such as search, scanner, catalogue, inventory, sales, trade, price check, AI, reports, settings, notifications, account, and support. Each capability may define:

- preferred existing global function(s)
- preferred existing section id(s)
- optional DOM action selector(s)
- safe fallback section

All sidebar, hero, quick-action, notification, profile, help, category, and report interactions will resolve through this adapter rather than ad-hoc direct routing.

### Honest navigation ownership

Where the underlying app has no dedicated page, the desktop shell will not pretend one exists. AI Insights and Reports will either open a real existing feature discovered during implementation or expose an intentional desktop panel/action built entirely from existing data. Search & Scan will provide both Universal Search and Scan Device access instead of silently behaving as only search.

### Live shell state

A `refreshDesktopState()` path will update shell-level data after route changes and existing Morley mutation events. It will avoid replacing legacy page DOM and only update desktop-owned elements. This includes notification badge visibility/count when discoverable, account identity text when discoverable, dashboard KPI/activity values, and any real-data overview summaries.

### Responsive lifecycle

The existing `parkLegacyHome()` / `restoreLegacyHome()` lifecycle remains. New state refresh/listeners must be idempotent, removed or harmless after teardown, and must not duplicate listeners across repeated >=1000px / <1000px transitions.

## Testing strategy

Use test-first contract coverage in `tests/morley-desktop-rebuild-contract.test.mjs` plus any focused new desktop parity contract file if the original becomes unwieldy. Required regression coverage:

- every visible sidebar item resolves to a real capability or declared intentional fallback
- Search & Scan exposes scanner and search paths
- notification and profile buttons have explicit behavior
- no hard-coded notification badge count
- category cards do not silently map unrelated device classes to Mobile Phones/General Buys
- reports do not masquerade as Sales when a report action is selected
- AI Insights does not silently masquerade as Home
- global search still transfers the query into an existing search field
- desktop teardown/restore preserves original Home nodes/listeners
- repeated resize cycles do not duplicate shell listeners
- physical phone and widths below 1000px remain excluded
- all new icon-only controls have accessible names

Repository-wide existing gates remain required: security audit, UI consistency/checklist, full feature contract, web smoke, layout contract, quality gate, ultimate parity, restore point, email contract where triggered, and the live desktop contract after deployment.

## Release strategy

Work on `hardening/morley-desktop-parity-20260913`. Keep commits narrow and reviewable. Open a PR to `main` only after regression tests and repository gates pass. Do not bypass protected review/merge boundaries. After merge, verify `Deploy B&L Morley Web`, post-deploy smoke tests, and `Web Desktop Live Contract` against the merged commit before declaring the pass complete.

## Out of scope

- Rewriting pricing algorithms
- Replacing authentication
- Changing Supabase schemas
- Rebuilding mobile UI
- Changing Guardian approval/protected-repair behavior
- Creating a separate AI backend
- Replacing the deployed hosting model
