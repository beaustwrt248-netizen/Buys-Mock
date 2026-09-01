# B&L Morley — Master UI / Theme / Layout Change Checklist

**Checklist version: 1.0**

This is the mandatory presentation regression checklist for B&L Morley, buyshub.me and Admin Control. Use it whenever a change can affect theme, colours, typography, spacing, sizing, layout, navigation, labels, icons, responsive behaviour, visibility, display state, accessibility or user-facing wording.

## Completion rule

For a UI/theme/layout PR, **every checkbox below must be considered**. Mark each applicable item verified. If an item is genuinely unaffected, mark it complete and record it as **N/A** in the PR evidence section. Never leave a relevant surface assumed-safe because it was not visible in the original bug report.

A presentation change is not complete until the relevant platform builds/tests are green and the PR contains `UI-CHECKLIST: COMPLETE`.

---

## 1. Global visual contract

- [ ] Canonical Morley light theme remains correct: soft light background, white surfaces, emerald accent, dark readable text.
- [ ] Dark surfaces (top bars/header areas) use light foreground text/icons with sufficient contrast.
- [ ] Primary, secondary, destructive, success, warning and disabled states are visually distinct.
- [ ] No retired gold/blue/cyan theme tokens reappear where Morley emerald is required.
- [ ] Font family is consistent across headings, body, labels, inputs, buttons, dialogs and status text.
- [ ] Font weight hierarchy is consistent: page title > section title > label/body > supporting/muted text.
- [ ] Text size remains readable on phones without browser zoom hacks.
- [ ] Line height, letter spacing and wrapping do not clip or overlap.
- [ ] Spacing, padding, margins, radii, borders and shadows are consistent.
- [ ] Icons align with labels and do not render as missing glyphs/emoji unexpectedly.
- [ ] All text/background combinations are readable in normal, hover/focus, selected, disabled and error states.
- [ ] No horizontal overflow at supported phone widths.
- [ ] Safe areas/system bars do not cover controls.
- [ ] Keyboard opening does not hide the active field/action.
- [ ] Scroll areas remain reachable and do not trap the user.
- [ ] Modal/dialog/backdrop layering and z-index remain correct.
- [ ] Loading, empty, offline, error, success and disabled states use the same visual system.

## 2. Global wording / spelling / formatting

- [ ] `B&L Morley` branding is consistent.
- [ ] `Computer Pricing` wording is consistent.
- [ ] `Laptop / MacBook` wording is consistent.
- [ ] `Desktop / Gaming PC` wording is consistent.
- [ ] `Console Pricing` wording is consistent.
- [ ] `General Buys / GP` wording is consistent.
- [ ] `Seller Ask` wording is consistent.
- [ ] `Max Buy` / protected buying terminology is consistent.
- [ ] `Buying Decision` wording is consistent.
- [ ] Australian English spelling is used for user-facing copy where applicable (for example `Analyse`).
- [ ] AUD currency formatting is correct and consistent.
- [ ] Capitalisation is consistent between navigation, titles, buttons, labels and help text.
- [ ] No known display typos, doubled words, truncation or accidental lowercase/uppercase regressions.
- [ ] Placeholder text, helper text, validation errors and toast messages are proofread.
- [ ] Dynamic values do not create awkward concatenation, missing spaces or duplicate punctuation.

## 3. Authentication / account shell

- [ ] Sign-in page layout and background render correctly.
- [ ] Email field is readable and correctly labelled.
- [ ] Password field is securely obscured and visually correct.
- [ ] CAPTCHA / Turnstile area remains visible and correctly positioned where required.
- [ ] Sign-in enabled/disabled state is obvious.
- [ ] Authentication error and success messages are readable.
- [ ] Loading/auth-verification state is readable.
- [ ] Signed-in account/user pill remains readable.
- [ ] Sign-out action remains visible and correctly styled.
- [ ] Account/profile labels and display name do not overflow.

## 4. Main dashboard / home

- [ ] Header/top bar.
- [ ] Welcome/account area.
- [ ] Primary navigation.
- [ ] Desktop side navigation.
- [ ] Mobile bottom navigation.
- [ ] Home hero/title/copy.
- [ ] Home tiles/cards.
- [ ] Live Pricing status.
- [ ] Online/Offline status.
- [ ] Connection/offline banner.
- [ ] Quick actions.
- [ ] Recent/saved/deal summary surfaces where present.
- [ ] Footer/copyright where present.

## 5. Computer Pricing — entry screen

- [ ] Computer Pricing title and explanatory copy.
- [ ] Laptop / MacBook choice card.
- [ ] Desktop / Gaming PC choice card.
- [ ] Choice-card icons, labels and helper copy.
- [ ] Selected/focus/pressed states.
- [ ] Back/navigation behaviour and visual active state.

## 6. Laptop / MacBook pricing

- [ ] Page title/header.
- [ ] Brand/manufacturer selector where present.
- [ ] Model/model-code input.
- [ ] Chromebook selection/catalog controls where present.
- [ ] MacBook selection/model controls where present.
- [ ] Specification fields and selectors.
- [ ] Seller Ask input.
- [ ] Grade selector.
- [ ] Analyse/valuation action.
- [ ] Loading/pricing state.
- [ ] Exact match / similar match / unavailable status.
- [ ] New/retail reference price display.
- [ ] Used/market price display.
- [ ] Comparable evidence list/cards.
- [ ] Evidence quality/confidence display.
- [ ] Max Buy display.
- [ ] Suggested/expected sale value display.
- [ ] Profit / GP / margin display.
- [ ] Buying Decision panel.
- [ ] BUY / NEGOTIATE / PASS-or-REJECT verdict state.
- [ ] Validation/error/offline state.
- [ ] Save/history/add-to-inventory actions where present.

## 7. Desktop / Gaming PC pricing

- [ ] Page title/header.
- [ ] OEM/model lookup.
- [ ] CPU input/selector.
- [ ] GPU input/selector.
- [ ] RAM input/selector.
- [ ] Storage input/selector.
- [ ] Motherboard / PSU / case / other component fields where present.
- [ ] Component rows do not overflow or misalign.
- [ ] Seller Ask input.
- [ ] Grade selector.
- [ ] Analyse/valuation action.
- [ ] Loading/pricing state.
- [ ] Retail/new value display.
- [ ] Used/market value display.
- [ ] Comparable evidence and match-quality display.
- [ ] Max Buy display.
- [ ] Profit / GP / margin display.
- [ ] Buying Decision panel and verdict.
- [ ] Validation/error/offline state.
- [ ] Save/history/add-to-inventory actions where present.

## 8. Console Pricing

- [ ] Console Pricing title/header.
- [ ] Console selector.
- [ ] PS4 variants.
- [ ] PS5 variants.
- [ ] Xbox One variants.
- [ ] Xbox Series variants.
- [ ] Nintendo Switch variants.
- [ ] Nintendo Switch 2 where supported.
- [ ] Grade A selector/state.
- [ ] Grade B selector/state.
- [ ] Grade C selector/state.
- [ ] Price-sheet/reference value.
- [ ] Auto Buy Price.
- [ ] Calculation/explanation text.
- [ ] Supported-console list rows.
- [ ] Selection and scroll-to-result behaviour.

## 9. General Buys / GP

- [ ] Page title/header.
- [ ] Item/category input.
- [ ] Price/value input.
- [ ] Seller Ask input where present.
- [ ] A Grade target/display.
- [ ] B Grade target/display.
- [ ] C Grade target/display.
- [ ] Luxury target/display.
- [ ] GP/margin explanation.
- [ ] Auto-calculated values.
- [ ] Buying Decision/verdict presentation.
- [ ] Error/invalid input state.

## 10. Quick Deal / Smart Workspace

- [ ] Quick Deal entry card.
- [ ] Item/model input.
- [ ] Seller Ask.
- [ ] Market/value field.
- [ ] Max Buy field.
- [ ] Grade selection.
- [ ] Target GP/margin.
- [ ] Smart insights.
- [ ] Metrics/cards.
- [ ] Recent items/deals.
- [ ] Decision/verdict state.
- [ ] Dialogs and close/back controls.

## 11. Test & Buy / device checks

- [ ] Test & Buy entry/navigation.
- [ ] Category/device test list.
- [ ] Pass/fail/unknown states.
- [ ] Required-test warnings.
- [ ] Failed-test impact on Buying Decision.
- [ ] Buttons/toggles/check controls.
- [ ] Long test names wrap correctly.
- [ ] Completion summary.

## 12. Valuation evidence / decision system

- [ ] Exact match labelling.
- [ ] Similar/spec match labelling.
- [ ] Match-quality indicator.
- [ ] Confidence indicator.
- [ ] Comparable titles and prices.
- [ ] Source labels.
- [ ] Evidence unavailable state.
- [ ] Low-confidence warning state.
- [ ] Seller Ask value.
- [ ] Market/expected sale value.
- [ ] Max Buy value.
- [ ] Target GP/margin.
- [ ] Profit amount.
- [ ] Buying Decision metrics.
- [ ] Verdict colour + text are both understandable; colour is never the only signal.

## 13. Inventory

- [ ] Inventory entry/navigation.
- [ ] Inventory list.
- [ ] Search/filter controls.
- [ ] Empty inventory state.
- [ ] Add item form.
- [ ] Edit item form.
- [ ] Item name/model.
- [ ] Barcode/serial fields.
- [ ] Category/platform fields where present.
- [ ] Condition/grade.
- [ ] Quantity.
- [ ] Cost/buy price.
- [ ] Market/RRP/reference price where present.
- [ ] Selling price.
- [ ] Profit/GP display.
- [ ] Save/update/delete controls.
- [ ] Confirmation/error states.

## 14. Barcode / scanner

- [ ] Scanner entry/navigation.
- [ ] Camera/scanner viewport where supported.
- [ ] Permission state.
- [ ] Start/stop controls where present.
- [ ] Manual barcode input.
- [ ] Scan result card.
- [ ] Matched-stock result.
- [ ] No-match result.
- [ ] Error/unavailable state.
- [ ] Scanner does not create horizontal overflow or duplicate screen layers.

## 15. Sales / realised profit

- [ ] Sales entry/navigation.
- [ ] Sales list/history.
- [ ] Search/filter controls.
- [ ] Revenue.
- [ ] Fees.
- [ ] Postage.
- [ ] Cost.
- [ ] Actual profit.
- [ ] GP/margin where present.
- [ ] Add/edit sale form.
- [ ] Empty/error states.

## 16. Saved valuations / history

- [ ] Saved/history navigation.
- [ ] List/card layout.
- [ ] Model/item title.
- [ ] Date/time formatting.
- [ ] Grade.
- [ ] Seller Ask.
- [ ] Market/value.
- [ ] Max Buy.
- [ ] Decision/verdict.
- [ ] Open/delete/restore actions where present.
- [ ] Empty state.

## 17. Menu / More / Help

- [ ] Menu trigger.
- [ ] Menu panel/card.
- [ ] Menu rows/icons/chevrons.
- [ ] Help & FAQ.
- [ ] Help dialog title and close control.
- [ ] Help section typography.
- [ ] FAQ expand/collapse controls.
- [ ] Diagnostics link.
- [ ] Notifications link.
- [ ] Updates link.
- [ ] Report an Issue / Support link.
- [ ] Account/sign-out controls.
- [ ] Admin-only options remain appropriately hidden/visible by role.

## 18. Notifications

- [ ] Notification centre entry.
- [ ] Unread/read styling.
- [ ] Notification title/body.
- [ ] Timestamp.
- [ ] Empty state.
- [ ] Error/loading state.
- [ ] Open/dismiss/mark-read actions where present.
- [ ] Android notification permission state where applicable.

## 19. Diagnostics / connection status

- [ ] Diagnostics entry.
- [ ] Online/offline state.
- [ ] Backend/service status rows.
- [ ] App/web version display.
- [ ] Device information where present.
- [ ] Healthy/warning/error colour and text.
- [ ] Refresh/retry controls.
- [ ] Long diagnostic values wrap without clipping.

## 20. Support / Report an Issue

- [ ] Support entry/navigation.
- [ ] Support top app bar/header.
- [ ] Tickets list.
- [ ] Ticket detail.
- [ ] New ticket form.
- [ ] Category selector.
- [ ] Subject field.
- [ ] Description field.
- [ ] Include diagnostics control.
- [ ] Attachment picker/name display.
- [ ] Submit state.
- [ ] Success ticket ID/status.
- [ ] Message/reply thread.
- [ ] Reply field/action.
- [ ] Unread admin-reply indication where present.
- [ ] Loading/empty/error states.
- [ ] Back/Close/Refresh actions.

## 21. Updates / release delivery

- [ ] Updates entry/navigation.
- [ ] Current version display.
- [ ] Available version display.
- [ ] Release notes.
- [ ] Update available/up-to-date/error state.
- [ ] Download/install action presentation.
- [ ] Progress/loading state.
- [ ] Signature/checksum or verification messaging where exposed.
- [ ] Web does not falsely present Android-only install capability.

## 22. Android NFC

- [ ] NFC entry/navigation where present.
- [ ] NFC unavailable state.
- [ ] NFC disabled state.
- [ ] Ready-to-scan state.
- [ ] Read result.
- [ ] Repeated-read/debounce behaviour does not duplicate UI.
- [ ] Text/URI record display.
- [ ] NFC remains read-only and does not imply inventory mutation.

## 23. Responsive web — phone

- [ ] 360 px width.
- [ ] 390 px width.
- [ ] 412–430 px width.
- [ ] Portrait orientation.
- [ ] Landscape orientation where practical.
- [ ] Bottom navigation fits.
- [ ] Four primary nav destinations remain usable.
- [ ] Cards stay within viewport.
- [ ] Buying Decision metrics stack correctly.
- [ ] Inputs remain at least touch/readability size.
- [ ] Dialogs fit viewport and scroll internally when needed.
- [ ] Sticky/fixed controls do not cover content.

## 24. Responsive web — tablet / desktop

- [ ] Tablet breakpoint.
- [ ] Desktop side navigation.
- [ ] Main content max width/alignment.
- [ ] Multi-column grids.
- [ ] Dialog width and positioning.
- [ ] Header/account area.
- [ ] Hover states do not replace keyboard focus states.
- [ ] Resizing between breakpoints does not leave stale inline styles/classes.

## 25. Accessibility / interaction

- [ ] Focus-visible state on interactive controls.
- [ ] Logical keyboard navigation order on web.
- [ ] Buttons have readable labels.
- [ ] Icon-only controls have accessible labels.
- [ ] Dialogs have title/role and a reliable close path.
- [ ] Form labels remain associated/clear.
- [ ] Error messages identify the affected field/action.
- [ ] Contrast remains readable for muted/supporting text.
- [ ] Colour is not the sole carrier of status/decision meaning.
- [ ] Touch targets are appropriately sized on Android/mobile web.
- [ ] Dynamic content updates remain understandable and do not visually jump unnecessarily.

## 26. Admin Control — authentication

- [ ] Admin login shell/theme.
- [ ] Email field.
- [ ] Secure password field.
- [ ] Turnstile CAPTCHA.
- [ ] Sign-in enable/disabled state.
- [ ] Error/loading state.
- [ ] Role/access-denied state.

## 27. Admin Control — dashboard / navigation

- [ ] Dashboard cards/metrics.
- [ ] Admin/Manager/Staff role-specific navigation.
- [ ] Mobile Admin navigation.
- [ ] Desktop Admin navigation.
- [ ] User/account/sign-out area.
- [ ] Loading/empty/error states.

## 28. Admin Control — support tickets

- [ ] Ticket list.
- [ ] Search/filter.
- [ ] Assignment control.
- [ ] Status control.
- [ ] Priority control.
- [ ] SLA indicators.
- [ ] Ticket detail.
- [ ] Protected messages.
- [ ] Reply controls.
- [ ] Staff assigned-only presentation.
- [ ] Admin/Manager-only controls are visually restricted correctly.
- [ ] Audit/history information where present.

## 29. Admin Control — users / devices

- [ ] User list.
- [ ] Display name.
- [ ] Email/account identity.
- [ ] Role display/edit control.
- [ ] User state/actions.
- [ ] Device list/details.
- [ ] App version visibility.
- [ ] Last-seen/status information where present.
- [ ] Confirmation/destructive states.

## 30. Admin Control — invitations / notifications / releases

- [ ] Invite form.
- [ ] Invite name/email/role fields.
- [ ] Invite list/status.
- [ ] Targeted notification form.
- [ ] Recipient/segment selection.
- [ ] Notification preview/status.
- [ ] Release control screen.
- [ ] Current/target version.
- [ ] Release safety/verification status.
- [ ] Remote configuration controls where present.
- [ ] Dangerous/powerful controls are visually differentiated and role gated.

## 31. Admin Control — audit / governance

- [ ] Audit log list.
- [ ] Audit detail.
- [ ] Actor/action/target/date formatting.
- [ ] Triage filters.
- [ ] Governance/status indicators.
- [ ] Empty/error/loading states.
- [ ] Protected/security information is not exposed to unauthorised roles.

## 32. Platform parity review

- [ ] Web and Android use the same product names.
- [ ] Web and Android use the same Morley light/emerald visual identity.
- [ ] Shared workflows have equivalent information hierarchy.
- [ ] Shared decision terminology matches.
- [ ] Shared grade terminology matches.
- [ ] Shared Seller Ask / Max Buy terminology matches.
- [ ] Platform-specific functions are clearly platform-specific instead of being falsely emulated.
- [ ] Android NFC remains Android-only.
- [ ] Android update/install UI remains Android-specific.
- [ ] Admin-only functions do not leak into standard Morley surfaces.

## 33. State matrix — mandatory for every affected component

For every affected control/card/screen, verify each state that exists:

- [ ] Default.
- [ ] Focused.
- [ ] Pressed/active.
- [ ] Selected.
- [ ] Disabled.
- [ ] Loading.
- [ ] Empty.
- [ ] Success.
- [ ] Warning.
- [ ] Error.
- [ ] Offline/unavailable.
- [ ] Long text / large value.
- [ ] Small phone width.
- [ ] Desktop width.

## 34. Final release evidence

- [ ] Changed files contain presentation changes only if the PR is declared presentation-only.
- [ ] No unintended pricing/valuation logic change.
- [ ] No unintended auth/CAPTCHA change.
- [ ] No unintended Supabase/RLS/ownership change.
- [ ] No unintended NFC behaviour change.
- [ ] No unintended Admin permission/role change.
- [ ] No unintended OTA/version/release metadata change.
- [ ] UI consistency audit passes.
- [ ] Quality gate passes.
- [ ] Web smoke checks pass when web is affected.
- [ ] Android build/tests/parity gates pass when Android is affected.
- [ ] Security checks pass.
- [ ] Exact merged SHA is verified after merge.
- [ ] Production deployment is verified when web is affected.
- [ ] OTA/release claims are made only with manifest + release + artifact checksum evidence.

---

## Keeping this checklist complete

Whenever a new screen, feature, setting, dialog, navigation destination, status card or user-visible state is added, **this file must be updated in the same PR**. The checklist is therefore an inventory of the product, not a one-time audit document.
