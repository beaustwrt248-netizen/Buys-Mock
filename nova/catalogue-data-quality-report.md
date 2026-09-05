# Nova Catalogue Data Quality Report

Checked: 2026-09-05 16:39:52 UTC

Catalogue latest row update observed in the same snapshot: 2026-09-05 16:38:26 UTC.

Source: one read-only atomic aggregate statement against `public.device_catalog`. All totals below come from the same database snapshot. No pricing fields, user data, destructive writes, schema changes, RLS changes or protected actions were performed.

## Point-in-time health

- Total records: 1,320
- Active records: 1,302
- AU/Australia-mapped records: 1,046
- Active records missing `model_number`: 165
- Active non-wearable records missing actionable storage: 37
- Active records missing `source_url`: 0

The catalogue is being modified independently of this report. These values are authoritative for the stated snapshot time only; Nova must not imply that a committed JSON snapshot is a continuously live database view.

## Category gaps

| Category | Active | Total | Missing model number | Missing actionable storage | AU/Australia mapped |
| --- | ---: | ---: | ---: | ---: | ---: |
| Mobile phones | 444 | 454 | 54 | 10 | 335 |
| Tablets | 257 | 259 | 45 | 6 | 252 |
| Laptops | 253 | 253 | 18 | 0 | 176 |
| Wearables | 161 | 162 | 29 | N/A | 151 |
| Consoles | 102 | 106 | 15 | 21 | 75 |
| Desktops | 85 | 86 | 4 | 0 | 57 |

## Listing-derived contamination signal

Using the same atomic snapshot, a conservative read-only scan identified 85 active candidate rows that may be listing-derived rather than canonical device models. The signal includes Cash Converters-sourced rows plus model names containing condition/listing text such as battery-health percentages, cracked/broken/damaged wording, sold-as-is/no-warranty text, power/update failures, or explicit condition grades.

- 77 of the 85 candidates are sourced from Cash Converters.
- 34 of the 85 candidates are also missing `model_number`.

| Category | Candidates | Cash Converters sourced | Missing model number |
| --- | ---: | ---: | ---: |
| Mobile phones | 31 | 24 | 7 |
| Tablets | 21 | 20 | 10 |
| Wearables | 22 | 22 | 17 |
| Consoles | 11 | 11 | 0 |

The candidate set is intentionally conservative evidence, not a deletion list. Some retailer-sourced rows may still map cleanly to a canonical device after verification.

## Interpretation

The current model-number gap must not be treated as 165 canonical devices needing a guessed model number. Some rows are contaminated listing imports or duplicate/variant records and need verification against canonical devices first.

The safe order is:

1. Identify canonical match candidates using brand, normalized model name, category, storage, release year and source evidence.
2. Separate true missing metadata from listing-derived duplicate/condition rows.
3. Verify manufacturer/Australian model numbers and storage against primary sources where available.
4. Prepare a reversible production-data change set with exact record IDs, before/after values and rollback SQL.
5. Treat any production-data mutation as high risk and require explicit human authorization before execution.
6. Re-run one atomic aggregate statement after approved cleanup and refresh Nova's snapshot from that single database state.

## Protected boundary

This report is evidence only. It does not authorize deletion, deactivation, merging, pricing changes, auth/RLS changes, Guardian changes or any other production write.
