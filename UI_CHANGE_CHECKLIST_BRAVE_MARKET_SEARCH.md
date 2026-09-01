# B&L Morley — Brave-first Market Search Change Checklist

- [x] Brave remains server-side only; no API key is committed or packaged in Android.
- [x] eBay AU remains the used-market source.
- [x] Brave is primary for retail/web discovery.
- [x] SerpApi/Google Shopping is fallback-only and labelled as fallback.
- [x] Gumtree public indexed listings use Brave first and SerpApi only as fallback.
- [x] Facebook Marketplace public indexed listings use Brave first and SerpApi only as fallback.
- [x] Marketplace asking prices are not treated as sold-comparable authority.
- [x] Australia targeting remains enabled.
- [x] Laptop exact/similar/rejected evidence classification remains intact.
- [x] Existing valuation/Buying Decision/Max Buy rules remain unchanged.
- [x] Existing Supabase auth boundary remains required.
- [x] Source/provider wording reflects the provider actually used.
- [x] Fallback and source-error states remain non-destructive.
- [x] CI verifies source priority and secret hygiene.
- [x] Signed Android build/manual-device verification required before release completion.
