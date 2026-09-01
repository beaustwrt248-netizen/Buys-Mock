# Morley 2.15.24 — Brave-first market search

Morley Laptop / MacBook analysis now targets the authenticated `market-search-v2` Supabase Edge Function. Brave Search is the primary web/new-retail discovery provider, eBay AU remains the primary used-market source, and SerpApi/Google Shopping is fallback-only. Public indexed Gumtree and Facebook Marketplace listings are discovered Brave-first with SerpApi fallback; these asking-price listings remain supplemental discovery evidence and are not promoted to sold-comparable authority.

The provider/status text stays within the existing full-width Compose layout with no new fixed-width containers or responsive breakpoints; phone/tablet layout contracts are unchanged and live provider labels remain part of installable-build verification.

Release promotion remains gated on the exact-head security, OTA/version, Android validation, APK build, parity, UI checklist and distribution checks.
