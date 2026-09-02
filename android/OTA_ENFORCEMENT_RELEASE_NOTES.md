# OTA enforcement — 2.15.44

- Background OTA checks run every 6 hours instead of every 12 hours.
- The app still performs an immediate update check when scheduling starts.
- Any verified newer OTA release now opens the non-dismissible update gate for signed-in users.
- Existing minimum-supported-version policy remains supported.
- Transient network failures do not create a false lockout.
- APK URLs and SHA-256 values continue to be validated by `UpdateManager` before installation is offered.
