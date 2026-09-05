# Apple Percentage-Text Duplicate Cleanup Plan — 2026-09-06

Status: **Prepared only — no production mutation has been executed.**

This plan records the remaining active Apple catalogue duplicates whose model names contain percentage or battery-health listing text. Condition/failure-text rows are intentionally excluded because that cleanup subset is already owned by merged PR #843.

## Safety boundary

Any `device_catalog` deactivation, merge, reference remap, or production-data rewrite is a **high-risk production-data change** under Nova governance and requires explicit human approval for the exact ready change. This document is read-only planning evidence.

Protected pricing must not be altered, copied, deleted, or remapped as part of a generic catalogue cleanup. Rows with `device_buy_prices` references remain blocked until a separately reviewed pricing-safe migration is explicitly approved.

## Verified live baseline

The dependency columns relevant to this cleanup are:

- `inventory_items.device_catalog_id`
- `device_buy_prices.device_catalog_id`
- `device_buy_price_history.device_catalog_id`

At the latest read-only check, all nine percentage/battery-health rows had zero inventory references and zero pricing-history references. Eight also had zero current pricing references. One row had one current protected pricing reference.

## Group A — unreferenced duplicate deactivation candidates

These eight rows have a clean active target and zero inventory/pricing/pricing-history references. They are candidates for deactivation only after exact human approval and a final precondition recheck immediately before execution.

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

## Group B — protected pricing blocker

This row must **not** be deactivated or remapped by the generic cleanup because it has a live `device_buy_prices` reference.

| Dirty ID | Dirty model name | Clean target ID | Clean canonical model | AU model number | Dependency refs | Status |
|---:|---|---:|---|---|---|---|
| 1063 | iPhone 16 Pro Max / Batt Health 100% | 551 | iPhone 16 Pro Max | A3296 | inventory 0 / pricing **1** / history 0 | BLOCKED — protected pricing |

## Explicit exclusions

IDs 1070, 1075, and 1206 are not part of this plan. They are condition/failure-text rows handled separately by the cleanup work already merged in PR #843. Their presence must not be inferred from this document, and this plan does not authorize any action on them.

## Required execution preconditions for Group A

Immediately before any approved production write, all of the following must still be true for every candidate ID:

1. The dirty row exists and `active = true`.
2. The mapped clean target exists and `active = true`.
3. `inventory_items` reference count is zero.
4. `device_buy_prices` reference count is zero.
5. `device_buy_price_history` reference count is zero.
6. The dirty model name still matches the percentage/battery-health duplicate classification.
7. No pricing, approval, Guardian, auth/RLS, release, or permission boundary is being changed.
8. The exact approved ID set has not changed since human review.

If any precondition fails, abort the entire mutation and re-review the candidate set.

## Proposed guarded production mutation — NOT EXECUTED

The intended mutation for an explicitly approved Group A operation is **deactivation only**. Do not delete rows and do not rewrite names/model numbers.

```sql
-- HIGH-RISK PRODUCTION DATA CHANGE — PREPARED ONLY, DO NOT RUN WITHOUT
-- EXPLICIT APPROVAL FOR THIS EXACT ID SET AFTER A FRESH PRECONDITION CHECK.

begin;

do $$
begin
  if exists (
    select 1
    from (values (1049),(1050),(1051),(1054),(1058),(1059),(1060),(1062)) v(id)
    where exists (select 1 from public.inventory_items i where i.device_catalog_id=v.id)
       or exists (select 1 from public.device_buy_prices p where p.device_catalog_id=v.id)
       or exists (select 1 from public.device_buy_price_history h where h.device_catalog_id=v.id)
  ) then
    raise exception 'Apple percentage-text cleanup aborted: dependency precondition changed';
  end if;
end $$;

update public.device_catalog
set active = false,
    updated_at = now()
where id in (1049,1050,1051,1054,1058,1059,1060,1062)
  and active = true;

-- Expected affected-row count: exactly 8. Verify before commit.
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
where id in (1049,1050,1051,1054,1058,1059,1060,1062)
  and active = false;
commit;
```

Rollback does not restore arbitrary metadata edits because the proposed operation makes none.

## Validation after an approved execution

- All eight approved percentage-text rows are inactive and their clean targets remain active.
- ID 1063 remains unchanged unless a separate protected-pricing change is explicitly approved.
- IDs 1070, 1075, and 1206 remain outside this plan.
- No inventory, pricing, or pricing-history references are created, deleted, or moved by this operation.
- Catalogue health is refreshed from one consistent live aggregate query.
- Catalogue import validator and cleanup classifier remain green.
- No protected pricing values or approvals change.

## Evidence note

The live rows and target mappings were read from Supabase on 2026-09-06 Australia/Perth. Because catalogue normalization can change production state, this document must never substitute for the immediate pre-execution database recheck described above.
