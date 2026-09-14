# Nova Next Visual Validation

## Current state

Nova Next is in the audited **GO FOR PROMOTION REVIEW** state. The completed implementation includes the responsive mobile shell, splash and login, dashboard and drawer navigation, Chat, Tools, Tasks, Projects, Settings, Control Centre, launcher/PWA identity, accessibility protections, and isolated OTA/update handling.

Automated source and CI contracts cover the responsive/mobile layout rules, safe-area handling, route structure, accessibility semantics, reduced-motion behavior, PWA/offline assets, Android wrapper identity, and OTA isolation. Those checks establish implementation and regression evidence, but they do not replace observation on a real device.

## Visual acceptance

Before production promotion, compare the deployed `/nova-next/` build at representative mobile widths against the approved Nova AI mobile reference and check the interactive device behaviors that static CI cannot prove: touch and pressed states, keyboard/IME resizing, orientation changes, physical safe-area insets, drawer reachability, scrolling/overflow, focus movement, and launcher presentation.

Record those observations as **MANUAL-DEVICE** evidence. Any meaningful visual or interaction drift is a promotion blocker and must be repaired and revalidated before cutover.

## Promotion boundary

This record does not authorize production replacement or release. Production `/nova/`, production Android identity/signing/versioning, and signed OTA publication remain protected actions. Updating visual-validation evidence must not trigger a production cutover or publish an OTA release.
