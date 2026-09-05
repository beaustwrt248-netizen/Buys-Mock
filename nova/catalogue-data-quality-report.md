# Nova Catalogue Data Quality Report

Checked: 2026-09-05

Source: read-only aggregate queries against `public.device_catalog`. No pricing fields, user data, destructive writes, schema changes, RLS changes or protected actions were performed.

## Current live health

- Total records: 1,317
- Active records: 1,299
- AU/Australia-mapped records: 1,060
- Active records missing `model_number`: 165
- Active non-wearable records missing actionable storage: 38
- Active records missing `source_url`: 0

## Category gaps

| Category | Active | Missing model number | Missing actionable storage | AU/Australia mapped |
| --- | ---: | ---: | ---: | ---: |
| Mobile phones | 444 | 54 | 10 | 344 |
| Tablets | 257 | 45 | 6 | 254 |
| Laptops | 250 | 18 | 0 | 174 |
| Wearables | 161 | 29 | N/A | 152 |
| Consoles | 102 | 15 | 22 | 78 |
| Desktops | 85 | 4 | 0 | 58 |

## Listing-derived contamination signal

A conservative read-only scan identified 111 active records that look listing-derived rather than canonical device models. Signals included Cash Converters source rows and model names containing condition/listing text such as battery health percentages, cracked/screen notes, sold-as-is/no-warranty wording, or similar listing-specific suffixes.

| Source / category | Suspected rows |
| --- | ---: |
| Cash Converters Australia — mobile phones | 35 |
| Cash Converters Australia — wearables | 22 |
| Cash Converters Australia — consoles | 21 |
| Cash Converters Australia — tablets | 19 |
| Apple Support Australia — mobile phones with listing-style names | 11 |
| Apple Support Australia — tablets with listing-style names | 2 |
| Apple Support Australia + Cash Converters Australia — tablet | 1 |

## Interpretation

The current model-number gap must not be treated as 165 canonical devices needing a guessed model number. Some rows are contaminated listing imports or duplicate/variant records and need normalization or deactivation after matching to a canonical device.

The safe order is:

1. Identify canonical match candidates using brand, normalized model name, category, storage, release year and source evidence.
2. Separate true missing metadata from listing-derived duplicate/condition rows.
3. Verify manufacturer/Australian model numbers and storage against primary sources where available.
4. Prepare a reversible production-data change set with exact record IDs, before/after values and rollback SQL.
5. Treat the production-data mutation as high risk and require explicit human authorization before execution.
6. Re-run aggregate health queries after the approved cleanup and refresh Nova's snapshot from the resulting live state.

## Protected boundary

This report is evidence only. It does not authorize deletion, deactivation, merging, pricing changes, auth/RLS changes, Guardian changes or any other production write.
