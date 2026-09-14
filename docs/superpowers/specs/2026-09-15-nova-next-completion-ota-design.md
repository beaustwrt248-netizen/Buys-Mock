# Nova Next Completion + OTA Design

## Goal
Finish Nova Next as one coherent, production-ready Android/PWA application, repair the structural UI defects visible on Home, Chat, Tools, Tasks and More, create a Nova launcher identity, and reuse Morley's OTA infrastructure through a strictly isolated Nova release channel.

## Application shell
Nova Next keeps its existing application rather than being rebuilt. Home, Chat, Tools, Tasks and More use one responsive shell with shared header metrics, content width, typography, navigation and Android safe-area handling. Scrollable views reserve space for both the Nova navigation bar and device insets. The hamburger contains secondary/admin/settings destinations; the bottom navigation remains the five primary destinations.

## Chat
Chat uses three structural regions: shared header, independently scrollable conversation, and persistent composer immediately above bottom navigation. Empty-state quick actions disappear once a conversation is active. Long assistant responses wrap inside their bubbles and never enter status/navigation safe areas. Ordinary assistant messages use Nova's normal blue/navy language rather than error-red styling. Internal capability/operational policy copy is not rendered as ordinary conversation content; protected actions remain enforced by the existing authorization boundaries.

## Tools
All, Productivity, Content and Analysis are real filters over tool metadata. Search and category filtering compose predictably. Tool routes are validated, active states are accessible, and the final item remains reachable above bottom navigation. Icons use a coherent Nova set.

## More / Control Centre
Control Centre remains an advanced read-only operational surface where existing boundaries require it. Telemetry exposes loading, healthy, degraded, unavailable and retry states. Guardian, deployment, pricing, release and role-changing authority remains behind existing protected boundaries.

## Home, Tasks and responsive behaviour
The shared shell normalises spacing, headings and touch targets. Home controls use recognisable actions. Tasks and all long views reserve safe scroll space. Portrait, narrow mobile, keyboard-open and wider layouts must not overlap the system UI or app navigation.

## App icon
Nova uses the existing glowing blue-to-violet orb on deep navy as its primary launcher identity. Android receives adaptive foreground/background assets and launcher density outputs; PWA receives standard and maskable manifest icons. No generic lettermark replaces the orb.

## OTA
Nova reuses Morley's proven OTA transport/release infrastructure but has its own app identity, release namespace, versionCode/versionName stream, channel metadata, manifest, APK identity and signing/hash metadata. A Morley package can never satisfy a Nova update check. Native flow: check -> available metadata -> release notes -> download -> integrity/identity validation -> Android installer handoff. Explicit states cover up-to-date, offline, download failure, verification failure and retry. Existing signing, approval and protected release boundaries are preserved. PWA service-worker asset updates remain independent from APK OTA.

## Testing and release gates
Regression coverage includes status-bar overlap, bottom-nav overlap, chat composer positioning, long response wrapping, empty-state actions, tool filtering/search, route integrity, responsive widths, accessibility, icon/manifest contracts and Nova/Morley OTA isolation. Existing repository security, parity, feature-contract and quality gates remain required before merge/release.

## Non-goals
This work does not weaken Guardian or human-approval boundaries, merge Nova and Morley release identities, replace the current Nova Next product, or expose protected deployment/pricing/catalogue/user-role actions from the read-only client.