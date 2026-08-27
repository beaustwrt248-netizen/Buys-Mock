# Android PR read-only rollout checklist

- [x] Dedicated PR workflow uses `contents: read`.
- [x] No production signing secret names are referenced.
- [x] No release publisher action is present.
- [x] No OTA metadata write path is present.
- [x] External actions are pinned to immutable SHAs.
- [x] Unit tests, lint, and debug APK compilation remain covered.
- [ ] Replacement workflow passes on its own pull request.
- [ ] Phase 2 removes PR execution from the write-capable production workflow.
