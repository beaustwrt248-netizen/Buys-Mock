# Morley AI intelligence layer

This layer extends the unified Morley AI capability registry with bounded, read-only operational intelligence.

## Connected capabilities

- `guardian.diagnostics` — reads recent Guardian diagnosis/proposed-action state without executing repairs.
- `catalogue.summary` — reports active catalogue coverage and data-quality gaps.
- `catalogue.search` — searches active catalogue records without mutating them.
- `valuation.summary` — reports recent valuation, expected-profit and realised-profit signals.

## Safety boundaries

- All new capabilities in this layer are `read` risk.
- No catalogue, valuation, Guardian, release or support mutation is introduced.
- Existing protected pricing application remains approval-gated.
- Guardian repair execution remains human-approved.
- No permission, RLS, auth, secret or deployment authority is widened.
- Live authoritative sources remain preferred over Morley session memory.

## Next layer

Inventory/sales intelligence can attach after its authoritative live sources and access contracts are verified. Protected catalogue and valuation writes should be added only through server-enforced approval and audit paths, never by direct client-side mutation.
