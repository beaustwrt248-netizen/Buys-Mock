# Morley ecosystem architecture

## Canonical products

The Morley ecosystem has three user-facing products and one shared platform.

### Morley Buys — Operations

Morley Buys is the everyday operational surface for staff. It owns valuations, trade-ins, device lookup, staff workflows and operational views of canonical pricing/catalogue data. It must not grow privileged administration or autonomous repair authority.

### Morley Admin — Control Centre

Morley Admin owns privileged business administration: accounts and roles, catalogue administration, protected pricing controls, integrations, backups, audit, support administration, release controls and system health. Business administration belongs here rather than in Morley Buys or Nova.

### Nova by Morley — Intelligence & Automation

Nova is the single AI/operator product. It owns research, catalogue intelligence, pricing intelligence, image intelligence, support intelligence, monitoring, recommendations and permitted automation. Nova consumes Morley Core data and must not maintain a competing catalogue, pricing source or identity store.

## Morley Core — single source of truth

Morley Core is the shared platform boundary used by all products. Canonical domains are catalogue, pricing, identity/RBAC, media, audit/events, search, integrations, notifications and realtime events.

A change to a canonical record is published once through Morley Core and consumed by each applicable client. Product-specific copies may be caches or projections only; they are never authoritative.

## Guardian — Nova's protected enforcement layer

Guardian is not a fourth product or competing assistant. It is Nova's independent security, governance and enforcement layer.

The operating path is:

`Human -> Morley product -> Morley Core`

For Nova-controlled or protected work:

`Human Admin -> Nova -> Guardian enforcement -> Morley Core / repository / release systems`

Guardian remains independently authoritative for protected boundaries. Nova cannot disable, bypass or weaken it, cannot self-approve protected work, and cannot treat missing enforcement evidence as permission.

Existing `guardian_*` database objects, audit history and compatibility APIs remain intentionally named Guardian. Those names describe the enforcement boundary and are not a separate user-facing product.

## Human-gated actions

The following remain human-gated unless a narrower approved policy explicitly says otherwise: destructive deletes, user/role changes, auth/security changes, protected pricing writes/approval, production repair execution, release/deployment, OTA/signing, irreversible catalogue patch application, and any action Guardian classifies as protected.

## Migration rules

1. Do not create new standalone Guardian navigation or branding. User-facing Guardian status belongs inside Nova as Security/Guardian enforcement information.
2. New shared catalogue, pricing, auth, media, notification and realtime logic belongs behind Morley Core contracts rather than product-local duplicates.
3. Existing duplicate/parity modules are migrated only after tests prove their replacement is equivalent; no mass deletion based on filename alone.
4. Morley Buys stays operational, Morley Admin stays administrative, and Nova stays intelligence/automation.
5. Every privileged or automated write must be attributable through audit events to the authenticated human, product surface, action and enforcement result.
6. Realtime is the default propagation mechanism for canonical changes; polling is fallback/recovery rather than the primary sync architecture.

## Naming

User-facing names are **Morley Buys**, **Morley Admin**, and **Nova by Morley**. Guardian remains an internal/protected capability name and may appear inside Nova where its enforcement status or evidence is relevant.

The machine-readable counterpart to this document is `/morley-core.js`.
