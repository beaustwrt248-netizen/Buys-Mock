# Web Catalogue Image Fix

The Morley web catalogue now renders verified direct images from approved extensionless CDN URLs used by Google Store, Acer/Prezly, Microsoft dynamic media, and ASUS press assets.

The fix does not proxy or rewrite the underlying HTTP resource. It adds a fragment-only `.jpg` hint for these verified hosts so the existing safe-image renderer recognises the asset, while preserving lazy loading and the existing broken-image fallback.
