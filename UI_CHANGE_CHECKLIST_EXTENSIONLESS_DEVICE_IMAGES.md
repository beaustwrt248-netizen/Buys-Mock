# UI Change Checklist — Extensionless Device Images

- [x] Scope is limited to catalogue device imagery; no pricing, auth, permissions, navigation, or approval logic changed.
- [x] Existing HTTPS image URLs with normal image extensions remain unchanged.
- [x] Only verified extensionless image hosts already present in the live resolved catalogue are normalized for the web renderer.
- [x] Normalization adds a URL fragment only (`#morley.jpg`), so the HTTP resource path/query is unchanged while the existing web image safety check can recognise the asset.
- [x] Google Store, Acer press/Prezly, Microsoft dynamic media, and ASUS press assets are covered.
- [x] Failed-image fallback and lazy loading remain unchanged.
- [x] Regression coverage added in `tests/web-extensionless-device-images.test.mjs`.
- [x] Mobile and desktop web use the same central pricing catalogue rows, so behaviour stays in parity.
