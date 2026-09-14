# Central pricing migration supersession

## Status

`20260903143000_central_pricing_catalogue.sql` is historical repository evidence, not the current Morley pricing authority. The current protected pricing path is `device_catalog` plus `device_buy_prices`, with immutable audit history in `device_buy_price_history` and privileged writes through `admin-pricing-control`.

The historical migration remains in git and must not be silently deleted or rewritten.

## Fresh-environment behavior

A naive fresh bootstrap that blindly treats every historical migration as current architectural intent can create the obsolete `morley_catalogue_items` and `morley_price_history` tables alongside the protected `device_catalog` + `device_buy_prices` model. That is a reproducibility hazard because it can look like a second valid pricing authority even though current runtime code does not use it.

Repository tooling and release review must therefore treat the supersession manifest as authoritative metadata: `20260903143000_central_pricing_catalogue.sql` is superseded by `20260904083000_device_buy_pricing.sql`, and it must not be applied merely to match production migration history.

## Runtime authority

The supported pricing architecture is:

- canonical devices: `public.device_catalog`
- protected buy prices: `public.device_buy_prices`
- immutable pricing audit history: `public.device_buy_price_history`
- privileged pricing API: `supabase/functions/admin-pricing-control/index.ts`

Active runtime code must not read or write `morley_catalogue_items` or `morley_price_history`.

## Reconciliation rule

Do not apply the old central-pricing migration merely to match production migration history. Do not drop, rename, rewrite, or mark historical migrations as applied in production solely for parity.

If a future environment already contains the obsolete tables, reconciliation must be reviewed as a separate operation. Before any destructive or migration-history repair, verify table emptiness, dependency references, pricing-linkage safety, rollback/recovery behavior, and the protected Admin Pricing path. Production migration-history repair, schema removal, or pricing-authority changes require explicit approval.

No production migration-history repair is performed by this change.

## Validation contract

`tests/pricing-migration-supersession-contract.test.mjs` enforces that:

- the historical migration remains present;
- the supersession is explicit and points to the protected replacement migration;
- current Admin Pricing still uses `device_catalog`, `device_buy_prices`, and `device_buy_price_history`;
- active runtime code does not reference the obsolete pricing tables;
- production repair remains approval-gated.
