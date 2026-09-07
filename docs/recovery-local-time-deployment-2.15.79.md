# Deployment plan

1. Run protected PR checks on the latest `main`-based branch.
2. Merge only after repository rules permit the merge.
3. Let the main Android workflow produce and sign the 2.15.79 APK.
4. Let the OTA workflow create the immutable release and OTA manifest promotion PR.
5. Verify `ota/latest.json` points to versionCode 123 / versionName 2.15.79 before considering deployment complete.
