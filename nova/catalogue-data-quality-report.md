# Nova Catalogue Data Quality Report

Checked: 2026-09-06 00:40 Australia/Perth

Source: the read-only aggregate snapshot represented by `nova/catalogue-health.json`. No pricing fields, user data, destructive writes, schema changes, RLS changes or protected actions were performed.

## Point-in-time health

- Total records: 1,320
- Active records: 1,302
- AU-region active records: 1,002
- Active records missing `model_number`: 165
- Active non-wearable records missing actionable storage: 37
- Active records missing `source_url`: 0

This is a point-in-time snapshot, not a continuously live database view. The catalogue is being modified independently as retailer-discovered rows are re-sourced and verified, so later live counts can differ from this committed evidence.

## Category gaps

| Category | Active | Total | Missing model number | Missing actionable storage | AU-region active |
| --- | ---: | ---: | ---: | ---: | ---: |
| Mobile phones | 444 | 454 | 54 | 10 | 328 |
| Tablets | 257 | 259 | 45 | 6 | 252 |
| Laptops | 253 | 253 | 18 | 0 | 153 |
| Wearables | 161 | 162 | 29 | N/A | 151 |
| Consoles | 102 | 106 | 15 | 21 | 68 |
| Desktops | 85 | 86 | 4 | 0 | 50 |

## Listing-derived contamination review

Earlier catalogue reviews demonstrated listing-derived contamination from retailer imports, especially Cash Converters rows and model names carrying condition or transaction text. Those historical findings explain the current cleanup work, but an old candidate count must not be presented as current after the database changes.

Use the repository's read-only review tooling against a fresh export when an exact candidate set is needed:

- `scripts/catalogue_import_validator.py` rejects listing/condition text and incomplete canonical identity before import.
- `scripts/catalogue_cleanup_audit.py` classifies existing exported rows for non-destructive review and verification.

Any candidate count used for a cleanup decision must come from one consistent current export or atomic database snapshot. It must not be inferred from the previous 111-row snapshot after the underlying catalogue has changed.

## Interpretation

The current model-number gap must not be treated as 165 canonical devices needing a guessed model number. Some rows can be listing-derived imports, duplicate/variant records, or otherwise incomplete and require verification against canonical device evidence first.

The safe order is:

1. Identify canonical match candidates using brand, normalized model name, category, storage, release year and source evidence.
2. Separate true missing metadata from listing-derived duplicate/condition rows.
3. Verify manufacturer/Australian-market model numbers and storage against primary sources where available.
4. Prepare a reversible production-data change set with exact record IDs, before/after values and rollback SQL.
5. Treat any production-data mutation as high risk and require explicit human authorization before execution.
6. Re-run one consistent aggregate/export after approved cleanup and refresh Nova's evidence from that resulting database state.

## Protected boundary

This report is evidence only. It does not authorize deletion, deactivation, merging, pricing changes, auth/RLS changes, Guardian changes or any other production write.
