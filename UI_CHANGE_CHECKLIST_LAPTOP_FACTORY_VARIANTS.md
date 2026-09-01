# B&L Morley — Laptop Factory Variant Change Checklist

This supplements `UI_CHANGE_MASTER_CHECKLIST.md` for every change to the Laptop / MacBook catalogue, guided picker, model identity, factory configuration data or laptop valuation-entry UI.

A laptop catalogue change is not complete until every applicable item below has been verified. Unsupported or unverified combinations must never be presented as manufacturer-verified.

## Model identity

- [ ] Brand is the correct manufacturer.
- [ ] Family/model name is correctly spelled and capitalised.
- [ ] Model year/generation is not presented as an exact factory identity unless verified.
- [ ] Exact version/model code is shown when verified (for example CX1400/CX1405CTA).
- [ ] Duplicate aliases/model codes resolve to one canonical identity where appropriate.
- [ ] Manufacturer source/provenance is retained for every verified profile.
- [ ] Retail observations are not treated as proof that no other factory variants exist.

## Dependent configuration choices

- [ ] Processor choices are limited to processors verified for the selected version/model code.
- [ ] RAM choices are limited to capacities produced for that selected version/model code.
- [ ] Storage choices are limited to capacities produced for that selected version/model code.
- [ ] Where exact processor/RAM/storage pairings are known, each downstream selector is filtered by all upstream selections.
- [ ] Changing model/version clears processor, RAM and storage selections.
- [ ] Changing processor clears incompatible RAM/storage selections.
- [ ] Changing RAM clears incompatible storage selections.
- [ ] An impossible configuration cannot enable `Analyse Laptop`.
- [ ] A verified configuration remains selectable and can reach market analysis.
- [ ] Legacy catalogue entries without factory verification are clearly labelled as unverified rather than manufacturer-verified.

## Search / valuation identity

- [ ] Verified model code is included in the canonical market query when available.
- [ ] Exact/similar/rejected evidence protections remain intact.
- [ ] Factory catalogue data never overrides market evidence, confidence, Max Buy or Buying Decision rules.
- [ ] Different generations remain excluded from exact valuation evidence.
- [ ] Seller Ask wording remains canonical.
- [ ] AUD formatting remains correct.

## Presentation / accessibility

- [ ] `Verified version / model code` selector is readable and reachable on phone layouts.
- [ ] Manufacturer verification/source label is readable without overpowering primary content.
- [ ] Disabled downstream selectors are visibly disabled.
- [ ] Primary `Analyse Laptop` button uses accessible foreground/background contrast.
- [ ] Loading, unavailable evidence and error states remain readable.
- [ ] Selected Configuration wraps without clipping or horizontal overflow.

## Regression evidence

- [ ] Unit test proves at least one known impossible configuration is rejected.
- [ ] Unit test proves a verified configuration is accepted.
- [ ] Unit test proves dependent RAM/storage filtering.
- [ ] Full Feature Contract Audit contains the factory-variant contract markers.
- [ ] Master UI checklist evidence is included in the PR.
- [ ] Real-device verification is recorded when practical.
