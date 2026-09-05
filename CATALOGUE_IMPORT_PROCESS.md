# Catalogue Import Process

Canonical `device_catalog` rows must not be created directly from retailer listing titles.
Retailer sources such as Cash Converters are useful for discovery and market evidence, but
manufacturer or Australian-market carrier/retailer evidence should be used to verify device
identity fields before canonical promotion.

## Required pre-import validation

1. Export candidate rows to JSON using the `device_catalog` field names.
2. Run `python3 scripts/catalogue_import_validator.py candidates.json`.
3. Resolve every validation issue. Missing model numbers must be verified; they must never be guessed.
4. When reviewing an existing catalogue export, run `python3 scripts/catalogue_cleanup_audit.py export.json` to classify rows for non-destructive review.
5. Only after validation should an authorized import path write canonical rows.

The validator rejects listing condition/transaction text from `model_name`, requires structured
storage where applicable, and requires category, manufacturer brand, model identity, and source URL.
The cleanup classifier flags suspicious model-number annotations and retailer-only identity evidence.
Neither tool connects to Supabase or performs writes.

## Production safety boundary

Changes that deactivate, delete, merge, or rewrite existing production catalogue rows are treated as
high-risk production-data changes. Prepare an exact before/after candidate set, evidence, validation,
and rollback plan first. Do not execute the mutation until the specific production change has received
human approval under the Nova/Guardian governance rules.

Pricing fields and pricing approvals are outside this import process and must not be changed as part of
catalogue identity cleanup.

## Source rules

- Manufacturer is the brand; a carrier is not a brand.
- Prefer Australian-market model numbers and variants.
- Keep tablets, wearables, consoles, laptops, desktops, and mobile phones in their own categories.
- Exclude 3G-only devices that are not suitable for the Australian network environment.
- Treat storage, colour, condition, store, item number, battery health, damage notes, and sale state as
  listing/inventory attributes unless they are genuine factory identity attributes.
- Do not infer a hardware model number from a retailer stock number, SKU, part number, or listing ID.
