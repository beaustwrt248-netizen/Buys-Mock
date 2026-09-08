# Morley Admin — Control Centre

Morley Admin is the privileged business-administration surface in the Morley ecosystem.

## Ownership

Morley Admin owns accounts and roles, catalogue administration, protected pricing controls, integrations, backups, audit, support administration, release controls and system health. Everyday valuation/trade-in work belongs in Morley Buys; AI research, intelligence, monitoring and automation belong in Nova by Morley.

## Shared platform

Admin consumes the same Morley Core source of truth as Morley Buys and Nova. Catalogue, pricing, identity/RBAC, media, audit/events, search, integrations, notifications and realtime events must not fork into Admin-specific authoritative copies.

## Nova and Guardian

Nova is the ecosystem's AI/operator product. Guardian is Nova's independent security, governance and enforcement layer, not a fourth user-facing application. Existing Guardian-named database/audit objects remain valid compatibility and enforcement names.

Protected actions remain human-gated and auditable. Admin must not provide a bypass around Guardian or grant Nova broader authority than the approved server-side policy.

See `../docs/MORLEY_ECOSYSTEM_ARCHITECTURE.md` and `../morley-core.js` for the canonical ecosystem contract.
