# Release evidence target

Expected final evidence after merge:
- main contains Android 2.15.79 / 123 and the local timestamp formatter.
- production Build B&L Morley APK workflow succeeds with signing verification.
- GitHub release `v2.15.79` exists with the signed APK.
- `ota/latest.json` on main advertises 2.15.79 / 123 and the release APK SHA-256.
