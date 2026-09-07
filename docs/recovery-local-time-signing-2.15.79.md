# Signing requirement

The PR build can validate compilation without production signing. After merge, the main release workflow must use the existing B&L Morley production keystore and verify the signer before publishing the 2.15.79 APK.
