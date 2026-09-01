## Summary

Describe what changed and why.

## Scope

- [ ] Changes are narrow and intentional.
- [ ] Required tests/checks for this change have been run or are running.

## UI / theme / layout changes

If this PR changes any theme, colour, font, spacing, layout, responsive behaviour, icon, label, user-facing wording or visual state, review the complete `UI_CHANGE_MASTER_CHECKLIST.md`.

For an affected item: verify it. For a genuinely unaffected item: record it as N/A in the evidence below. Do not assume a screen is safe because it was not part of the original bug report.

<!-- UI-CHECKLIST-START -->
- [ ] Global visual contract reviewed.
- [ ] Wording, spelling, capitalisation and AUD formatting reviewed.
- [ ] Authentication/account surfaces reviewed.
- [ ] Dashboard/home/navigation/status surfaces reviewed.
- [ ] Computer Pricing entry reviewed.
- [ ] Laptop / MacBook pricing reviewed.
- [ ] Desktop / Gaming PC pricing reviewed.
- [ ] Console Pricing reviewed.
- [ ] General Buys / GP reviewed.
- [ ] Quick Deal / Smart Workspace reviewed.
- [ ] Test & Buy / device checks reviewed.
- [ ] Valuation evidence / Buying Decision reviewed.
- [ ] Inventory reviewed.
- [ ] Barcode/scanner reviewed.
- [ ] Sales / realised profit reviewed.
- [ ] Saved valuations/history reviewed.
- [ ] Menu / More / Help reviewed.
- [ ] Notifications reviewed.
- [ ] Diagnostics / connection status reviewed.
- [ ] Support / Report an Issue reviewed.
- [ ] Updates / release-delivery UI reviewed.
- [ ] Android NFC presentation reviewed.
- [ ] Phone responsive layouts reviewed.
- [ ] Tablet/desktop responsive layouts reviewed.
- [ ] Accessibility and interaction states reviewed.
- [ ] Admin authentication reviewed.
- [ ] Admin dashboard/navigation reviewed.
- [ ] Admin support-ticket UI reviewed.
- [ ] Admin users/devices UI reviewed.
- [ ] Admin invites/notifications/releases UI reviewed.
- [ ] Admin audit/governance UI reviewed.
- [ ] Web to Android platform parity reviewed.
- [ ] State matrix reviewed for every affected component.
- [ ] Final release evidence reviewed.
<!-- UI-CHECKLIST-END -->

### UI checklist evidence

`UI-CHECKLIST: PENDING`

Affected surfaces:
- 

Explicit N/A areas and why:
- 

Widths/devices checked:
- 

Screenshots/manual observations when practical:
- 

Automated gates:
- 

Replace `UI-CHECKLIST: PENDING` with `UI-CHECKLIST: COMPLETE` only after every category above has been resolved and the detailed master checklist has been reviewed.

## Mobile web theme full-sweep rule

Any change to buyshub.me theme, CSS, mobile layout, responsive presentation, parity renderer, visual shell, or theme-sensitive web JavaScript triggers the mobile-web full-sweep gate. The check is repository-enforced: the PR cannot pass by checking only the page that was intentionally edited.

For a genuinely unaffected surface, mark the item complete and explicitly explain the N/A in the evidence section. Do not leave a box unchecked.

<!-- MOBILE-WEB-THEME-CHECKLIST-START -->
- [ ] Mobile Home/dashboard and all later dynamic renderers checked.
- [ ] Header, account pill, online/status surfaces and fixed/sticky elements checked.
- [ ] Mobile bottom navigation and safe-area spacing checked.
- [ ] Computer Pricing entry checked.
- [ ] Laptop / MacBook pricing screen checked.
- [ ] Desktop / Gaming PC pricing screen checked.
- [ ] Console Pricing screen checked.
- [ ] General Buys / GP screen checked.
- [ ] Quick Deal / Smart Workspace checked.
- [ ] Saved valuations/history and recent activity checked.
- [ ] Menu / More / Help and account actions checked.
- [ ] Authentication/sign-in surfaces checked.
- [ ] Notifications, diagnostics, support/report issue and updates checked.
- [ ] Loading, empty, offline, error, disabled and selected states checked.
- [ ] No legacy renderer, duplicate card, duplicate screen or hidden stale layer reappears after load.
- [ ] No retired blue/cyan/gold theme override defeats the canonical Morley light/emerald theme.
- [ ] 360 px mobile width checked.
- [ ] 390 px mobile width checked.
- [ ] 412–430 px mobile width checked.
- [ ] Portrait and practical landscape behaviour checked.
- [ ] No horizontal overflow, clipped text, unreachable controls or covered content.
- [ ] Touch targets, focus-visible state, keyboard behaviour, labels and contrast checked.
- [ ] Web-to-Android visual/navigation parity checked for all affected shared surfaces.
<!-- MOBILE-WEB-THEME-CHECKLIST-END -->

### Mobile web theme evidence

`MOBILE-WEB-THEME-CHECKLIST: PENDING`

Mobile web affected surfaces:
- 

Mobile web N/A surfaces and why:
- 

Mobile widths checked:
- 

Legacy/duplicate renderer result:
- 

Parity/visual result:
- 

Automated mobile-web gates:
- 

Replace `MOBILE-WEB-THEME-CHECKLIST: PENDING` with `MOBILE-WEB-THEME-CHECKLIST: COMPLETE` only when the full mobile web sweep has been completed for a theme-sensitive web change.
