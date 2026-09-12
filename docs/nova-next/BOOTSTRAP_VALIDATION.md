# Nova Next Bootstrap Validation

Status is updated only from observed checks.

## Completed contract checks

The dependency-free local suite completed successfully after correcting an isolation-test self-match:

- `layout-contract.test.mjs`: PASS
- `router.test.mjs`: PASS
- `auth-policy.test.mjs`: PASS
- `auth-adapter.test.mjs`: PASS
- `capability-registry.test.mjs`: PASS
- `action-policy.test.mjs`: PASS
- `nova-api.test.mjs`: PASS
- `promotion-config.test.mjs`: PASS
- `isolation.test.mjs`: PASS
- `node --check nova-next/app.js`: PASS
- `node --check nova-next/service-worker.js`: PASS

## Isolation verification

Repository comparison against `main` shows the bootstrap changes are additions only under:

- `nova-next/`
- `docs/nova-next/`
- `docs/superpowers/plans/2026-09-13-nova-next-standalone.md`

No existing `nova/`, `android/`, `supabase/`, `.github/` or other production application file is modified by this bootstrap diff.

No production Supabase schema/RLS/Edge Function, deployment, signing, OTA, pricing or Guardian authority change is included. Login in the bootstrap UI remains a visual preview until the reviewed production auth wiring is added.

## Promotion identity verification

Current production Nova identity was read from the existing configuration and recorded for future promotion checks:

- Android production application ID: `com.buysloans.nova`
- Current production web scope: `/nova/`
- Nova Next development application ID target: `com.buysloans.novanext`
- Nova Next development web scope: `/nova-next/`
- Nova Next development cache namespace: `nova-next-dev-v1`

Recording production identity does not activate it in Nova Next.

## Visual verification limitation

The implementation is based directly on the approved `Nova AI Mobile App UI Mockup.png` reference. Automated browser screenshot rendering could not be completed in this execution environment because navigation to both localhost and `file://` targets is blocked by administrator policy (`net::ERR_BLOCKED_BY_ADMINISTRATOR`). No screenshot-parity claim is made. A deployed/CI preview must receive visual acceptance before promotion.

## Branch state

At the final comparison, `nova-next/bootstrap` contains only the isolated bootstrap additions but is three commits behind the moving `main` branch. The review PR should therefore be checked against current `main` and updated/retested before merge if GitHub reports a merge conflict or stale required check.
