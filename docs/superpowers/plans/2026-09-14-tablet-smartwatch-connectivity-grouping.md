# Tablet and Smartwatch Connectivity Grouping Implementation Plan

**Goal:** Consolidate tablet and smartwatch connectivity variants in the Morley catalogue UI without deleting or merging authoritative catalogue rows.

**Architecture:** Keep source catalogue rows intact as the pricing/identity source of truth. Add a pure connectivity-normalisation helper plus a DOM grouping layer that wraps the already-bound result cards, so every Storage/Connectivity choice delegates to the exact original row and preserves SKU/model/pricing behaviour.

## Tasks

1. Add regression tests first for tablet and wearable connectivity parsing/group keys, including Wi‑Fi, Wi‑Fi + Cellular, 4G/5G, GPS, GPS + LTE, GPS + Cellular, LTE and Cellular aliases. Assert generation, watch size and edition/material stay in the base-model identity.
2. Add `catalogue-connectivity-core.js`, a browser/Node-compatible pure helper for category detection, connectivity extraction, canonical display labels, model cleanup, group keys and connectivity search aliases.
3. Add `tablet-watch-grouping.js` to post-process Universal Search tablet/wearable result cards. Group by brand + cleaned base model; preserve Storage and Connectivity as separate selectors; only expose valid storage/connectivity combinations; delegate Open Buy Flow/Favourite to the exact hidden source card.
4. Extend the existing mobile grouping layer so catalogue counts reflect grouped device cards rather than raw variant rows.
5. Add scoped CSS for tablet/smartwatch grouped cards and connectivity chips, reusing the existing Morley catalogue visual language.
6. Load the new core/grouping layers after `web-universal-buy.js` and cache-bust the updated grouping assets in `index.html`.
7. Verify focused Node regression tests, existing phone grouping regression, relevant catalogue identity-boundary tests, then inspect the branch diff and open a PR without merging.

## Safety boundaries

- No catalogue row deletion/deactivation.
- No pricing-table mutation.
- No inference that LTE implies GPS unless the source row explicitly says so.
- Different generations, sizes, editions/materials and model families remain separate.
- A visible selection must always map to an existing exact source row; impossible storage/connectivity combinations are never synthesized.
