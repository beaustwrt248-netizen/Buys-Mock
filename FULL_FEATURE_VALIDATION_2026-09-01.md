# B&L Morley — Full Feature Validation Pass

Baseline main SHA: `a4bd16b33450ce8f57ad9699a54d3f6a980e28af`
Tracker: #431
Checklist: `UI_CHANGE_MASTER_CHECKLIST.md`

## Status meanings

- **PASS (source/CI)** — source contract and applicable automated gates support the result.
- **PASS (manual evidence)** — supplied screenshot/device evidence supports the result.
- **DEFECT** — confirmed mismatch requiring a fix.
- **MANUAL-DEVICE** — interactive device/browser behaviour cannot be proven by static repository inspection alone.
- **IN PROGRESS** — audit not yet complete.
- **N/A** — genuinely not applicable, with reason.

## Validation ledger

| # | Area | Status | Evidence / finding |
|---|---|---|---|
| 1 | Global visual contract | PASS (source/CI) | Canonical final presentation layer `morley-ui-baseline.css/js`; UI consistency gate protects light theme, font, viewport and contrast tokens. |
| 2 | Global wording / spelling / formatting | DEFECT → FIX STAGED | Production base still contains `Seller asking price`; audit branch normalises this to canonical `Seller Ask`. Australian `Analyse` normalisation already enforced. |
| 3 | Authentication / account shell | IN PROGRESS | Auth-origin and UI consistency gates green on baseline; interactive keyboard/CAPTCHA layout still requires device verification. |
| 4 | Main dashboard / home | PASS (manual evidence + source/CI) | User screenshot confirmed startup workspace now renders immediately. Live Pricing / Online Status contrast is protected by canonical baseline. |
| 5 | Computer Pricing — entry | PASS (source/CI) | `product-parity-v3.js` creates canonical Computer Pricing choice surface with Laptop / MacBook and Desktop / Gaming PC. |
| 6 | Laptop / MacBook pricing | IN PROGRESS | Source markers and valuation parity exist; complete state/keyboard/device review pending. |
| 7 | Desktop / Gaming PC pricing | IN PROGRESS | Source markers and parity exist; complete component/state review pending. |
| 8 | Console Pricing | PASS (source/CI) | Canonical console section, PS4/PS5/Xbox/Switch/Switch 2 entries and A/B/C grade calculation presentation present; Ultimate Parity gate covers product parity. |
| 9 | General Buys / GP | IN PROGRESS | Canonical route/title present; calculation and state presentation review ongoing. |
| 10 | Quick Deal / Smart Workspace | IN PROGRESS | Workspace present; mobile/readability review ongoing. |
| 11 | Test & Buy / device checks | IN PROGRESS | Functional/state inventory pending. |
| 12 | Valuation evidence / Buying Decision | PASS (source/CI + manual evidence) | Canonical baseline explicitly protects Buying Decision metric/value/verdict contrast; user screenshot exposed old defect now covered. NFC/valuation regression gate green on prior tested head. |
| 13 | Inventory | IN PROGRESS | Base inventory/list/add/edit surfaces present; full visual/state review pending. |
| 14 | Barcode / scanner | MANUAL-DEVICE | Source scanner surfaces exist; camera permission, viewport and duplicate-layer behaviour require actual browser/device exercise. |
| 15 | Sales / realised profit | IN PROGRESS | Base sales/history surfaces present; full state review pending. |
| 16 | Saved valuations / history | IN PROGRESS | Runtime saved/recent valuation surfaces present; full state review pending. |
| 17 | Menu / More / Help | IN PROGRESS | More/menu/help parity files present; full role/state review pending. |
| 18 | Notifications | IN PROGRESS | Notification centre runtime exists; state review pending. |
| 19 | Diagnostics / connection status | IN PROGRESS | `connection-status.js` and diagnostics runtime exist; wrapping/error state review pending. |
| 20 | Support / Report an Issue | DEFECT | Android `MessageCard` renders user author/body with `Color.White` on light `SupportCard`; confirmed source defect in `SupportTicketActivity.kt`. Header contrast fix itself remains correct. |
| 21 | Updates / release delivery | PASS (source/CI) | OTA/version/release safety gates exist; 2.15.21 manifest/release was checksum-verified before this pass. Interactive install UI still requires device exercise when changed. |
| 22 | Android NFC | PASS (source/CI) | Ultimate Parity includes NFC/valuation regression; read-only boundary remains protected. |
| 23 | Responsive web — phone | PASS (manual evidence + source/CI) | 412/430-class screenshots show corrected phone scale; baseline enforces viewport/max-width/16px inputs. 360/390 and landscape remain manual follow-up. |
| 24 | Responsive web — tablet / desktop | IN PROGRESS | Desktop mode/layout runtime exists; breakpoint transition review pending. |
| 25 | Accessibility / interaction | IN PROGRESS | Focus-visible and a11y runtime present; keyboard order and dynamic-state manual review pending. |
| 26 | Admin Control — authentication | IN PROGRESS | CAPTCHA/security governance preserved; presentation review pending. |
| 27 | Admin Control — dashboard / navigation | IN PROGRESS | Admin app/dashboard present; role-specific visual pass pending. |
| 28 | Admin Control — support tickets | IN PROGRESS | Support governance/integration workflows present; role/assignment/SLA presentation review pending. |
| 29 | Admin Control — users / devices | IN PROGRESS | User/device governance source and checks present; visual/state review pending. |
| 30 | Admin Control — invites / notifications / releases | IN PROGRESS | Invites, targeted notifications and release controls present; visual/state review pending. |
| 31 | Admin Control — audit / governance | IN PROGRESS | Audit triage/control governance present; visual/state review pending. |
| 32 | Cross-platform parity | IN PROGRESS | Ultimate Parity gate protects core product parity; full label/state mapping review ongoing. |
| 33 | State matrix | IN PROGRESS | Loading/empty/error/success/disabled/offline/selected/focus states being checked per affected surface. |
| 34 | Final release verification | IN PROGRESS | Will only close after all confirmed defects are fixed, required CI is green, deployment/OTA evidence is exact, and remaining MANUAL-DEVICE items are explicitly recorded. |

## Confirmed defects

### FVP-001 — Android Support message contrast

`SupportTicketActivity.MessageCard` uses a light card background for both user and support messages, but the body is forced to `Color.White`, and the user author is also forced to `Color.White`. This is unreadable/low-contrast under the canonical light Morley theme.

Required correction: use canonical dark primary text for body/user author while retaining emerald emphasis for the support author. This is presentation-only; ticket auth, ownership, RLS, diagnostics and message flow must remain unchanged.

### FVP-002 — Seller Ask wording drift

The deployed legacy base document contains `Seller asking price` on laptop and desktop pricing. The master contract requires `Seller Ask`. The audit branch extends the final runtime normaliser so both legacy variants resolve to `Seller Ask` without modifying pricing inputs or calculations.

## Release rule for this pass

Do not mark the full validation pass complete merely because automated checks are green. Any `MANUAL-DEVICE` rows must either be exercised on a real target device/browser or remain explicitly recorded as manual evidence still required. Any `DEFECT` row must be fixed and retested before closure.
