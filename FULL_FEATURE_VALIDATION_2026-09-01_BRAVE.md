# Brave-first market search validation — 2026-09-01

Scope: Morley Android Laptop / MacBook market evidence and server-side market discovery.

- [x] Brave API key remains server-side in Supabase secret storage only.
- [x] Brave Search is primary for web/new-retail discovery.
- [x] SerpApi/Google Shopping is fallback-only for retail discovery.
- [x] eBay AU remains the primary used-market source.
- [x] Gumtree public indexed listings use Brave first, SerpApi fallback.
- [x] Facebook Marketplace public indexed listings use Brave first, SerpApi fallback.
- [x] Gumtree/Facebook are discovery evidence only and are not promoted to sold-comparable authority.
- [x] Android attribution no longer hard-codes Google when Brave is primary.
- [x] Provider/fallback status is surfaced in the Laptop / MacBook flow.
- [x] Existing exact/similar/rejected classification remains client-side for laptop valuation evidence.
- [x] Existing auth, Supabase RLS/auth boundaries, NFC and valuation decision logic remain unchanged.
- [x] CI contract checks verify the Brave endpoint, provider priority, marketplace coverage and secret hygiene.

Manual-device verification remains required after a signed 2.15.24 build is produced.
