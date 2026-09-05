# Apple Canonical Duplicate Cleanup Plan — 2026-09-06

Status: **Prepared only — no production mutation has been executed.**

This plan records the exact active Apple catalogue rows that still contain retailer/listing condition text after their evidence source was re-aligned to Apple Support. Every dirty row already has a separate clean active canonical equivalent, so the safe cleanup direction is duplicate deactivation rather than renaming dirty rows into collisions.

## Safety boundary

Any `device_catalog` deactivation, merge, reference remap, or production-data rewrite is a **high-risk production-data change** under Nova governance and requires explicit human approval for the exact ready change. This document is read-only planning evidence.

Protected pricing must not be altered, copied, deleted, or remapped as part of a generic catalogue cleanup. Rows with `device_buy_prices` references remain blocked until a separately reviewed pricing-safe migration is explicitly approved.

## Verified live baseline

The following dependency columns reference `device_catalog.id`:

- `inventory_items.device_catalog_id`
- `device_buy_prices.device_catalog_id`
- `device_buy_price_history.device_catalog_id`

At the latest read-only check, all 12 dirty rows had zero inventory references and zero pricing-history references. Ten also had zero current pricing references. Two rows had one current protected pricing reference each.

## Group A — unreferenced duplicate deactivation candidates

These ten rows have a clean active target and zero inventory/pricing/pricing-history references. They are candidates for deactivation only after exact human approval and a final precondition recheck immediately before execution.

| Dirty ID | Dirty model name | Clean target ID | Clean canonical model | AU model number | Dependency refs |
|---:|---|---:|---|---|---|
| 1049 | iPhone 11 79% | 537 | iPhone 11 | A2221 | inventory 0 / pricing 0 / history 0 |
| 1050 | iPhone 11 Pro 75% | 538 | iPhone 11 Pro | A2215 | 0 / 0 / 0 |
| 1051 | iPhone 12 Pro 74% | 543 | iPhone 12 Pro | A2407 | 0 / 0 / 0 |
| 1054 | iPhone 13 Pro Max - - 77% | 548 | iPhone 13 Pro Max | A2643 | 0 / 0 / 0 |
| 1058 | iPhone 15 85% | 523 | iPhone 15 | A3090 | 0 / 0 / 0 |
| 1059 | iPhone 15 Plus - - 90% | 525 | iPhone 15 Plus | A3094 | 0 / 0 / 0 |
| 1060 | iPhone 16 Plus - - 100% | 550 | iPhone 16 Plus | A3290 | 0 / 0 / 0 |
| 1062 | iPhone 16 Pro - - 93% | 35 | iPhone 16 Pro | A3293 | 0 / 0 / 0 |
| 1070 | iPhone 8 Plus "Cracked Screen/Cant Update" | 645 | iPhone 8 Plus | unverified | 0 / 0 / 0 |
| 1075 | iPhone Xs Max - Back Cracked | 626 | iPhone XS Max | unverified | 0 / 0 / 0 |

The clean iPhone 8 Plus and iPhone XS Max targets are already active canonical rows but still have their own model-number verification gaps. Deactivating the dirty duplicates does **not** authorize guessing or filling those model numbers.

## Group B — protected pricing blockers

These rows must **not** be deactivated or remapped by the generic cleanup because each has a live `device_buy_prices` reference.

| Dirty ID | Dirty model name | Clean target ID | Clean canonical model | AU model number | Dependency refs | Status |
|---:|---|---:|---|---|---|---|
| 1063 | iPhone 16 Pro Max / Batt Health 100% | 551 | iPhone 16 Pro Max | A3296 | inventory 0 / pricing **1** / history 0 | BLOCKED — protected pricing |
| 1206 | iPad Air 2 Doesnt Turn On At All Sold As Is No Warranty | 1204 | iPad Air 2 | unverified | inventory 0 / pricing **1** / history 0 | BLOCKED — protected pricing |

For ID 1206, the clean iPad Air 2 target is active but its AU model number is not yet verified. That metadata gap is independent of the duplicate cleanup.

## Required execution preconditions for Group A

Immediately before any approved production write, all of the following must still be true for every candidate ID:

1. The dirty row exists and `active = true`.
2. The mapped clean target exists and `active = true`.
3. `inventory_items` reference count is zero.
4. `device_buy_prices` reference count is zero.
5. `device_buy_price_history` reference count is zero.
6. The dirty model name still matches a catalogue listing-text rejection rule.
7. No pricing, approval, Guardian, auth/RLS, release, or permission boundary is being changed.
8. The exact approved ID set has not changed since human review.

If any precondition fails, abort the entire mutation and re-review the candidate set.

## Proposed guarded production mutation — NOT EXECUTED

The intended mutation for an explicitly approved Group A operation is **deactivation only**. Do not delete rows and do not rewrite names/model numbers.

```sql
-- HIGH-RISK PRODUCTION DATA CHANGE — PREPARED ONLY, DO NOT RUN WITHOUT
-- EXPLICIT APPROVAL FOR THIS EXACT ID SET AFTER A FRESH PRECONDITION CHECK.

begin;

-- Recheck that none of the candidate rows acquired references.
do $$
begin
  if exists (
    select 1
    from (values (1049),(1050),(1051),(1054),(1058),(1059),(1060),(1062),(1070),(1075)) v(id)
    where exists (select 1 from public.inventory_items i where i.device_catalog_id=v.id)
       or exists (select 1 from public.device_buy_prices p where p.device_catalog_id=v.id)
       or exists (select 1 from public.device_buy_price_history h where h.device_catalog_id=v.id)
  ) then
    raise exception 'Apple duplicate cleanup aborted: dependency precondition changed';
  end if;
end $$;

update public.device_catalog
set active = false,
    updated_at = now()
where id in (1049,1050,1051,1054,1058,1059,1060,1062,1070,1075)
  and active = true;

-- Expected affected-row count: exactly 10. Verify before commit.
-- If the count or resulting state is unexpected: ROLLBACK.

commit;
```

The final approved execution should also capture a before-state snapshot in the audit record before mutation and verify the exact affected-row count before commit. If the execution interface cannot safely enforce those checks in one controlled transaction, do not run it.

## Rollback plan

Because Group A is deactivation-only and has no references at the approved execution point, rollback is reversible by reactivating the exact same IDs after confirming no conflicting canonical state was introduced:

```sql
-- ROLLBACK TEMPLATE — use only for the exact executed cleanup set.
begin;
update public.device_catalog
set active = true,
    updated_at = now()
where id in (1049,1050,1051,1054,1058,1059,1060,1062,1070,1075)
  and active = false;
commit;
```

Rollback does not restore arbitrary metadata edits because the proposed operation makes none.

## Validation after an approved execution

- All ten approved dirty rows are inactive and their clean targets remain active.
- IDs 1063 and 1206 remain unchanged unless a separate protected-pricing change was explicitly approved.
- No inventory, pricing, or pricing-history references were created, deleted, or moved by this operation.
- Catalogue health is refreshed from one consistent live aggregate query.
- Catalogue import validator and cleanup classifier remain green.
- No protected pricing values or approvals change.

## Evidence note

The live rows and target mappings were read from Supabase on 2026-09-06 Australia/Perth. Because another catalogue-normalization process has been active, this document must never substitute for the immediate pre-execution database recheck described above.
