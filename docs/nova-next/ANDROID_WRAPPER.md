# Nova Next Android Wrapper

Nova Next includes a standalone development Android project under `nova-next/android/`. It is deliberately independent of the existing `android/novaapp/` production Nova project.

## Development identity

- Application ID: `com.buysloans.novanext`
- Debug application ID: `com.buysloans.novanext.debug`
- Display name: `Nova Next`
- Web surface: `https://buyshub.me/nova-next/`

The production Nova application ID `com.buysloans.nova`, production signing key and production version stream are not used by this wrapper.

## WebView boundary

The wrapper:

- permits only HTTPS Nova Next top-level navigation on `buyshub.me` / `www.buyshub.me` under `/nova-next`;
- opens other HTTPS top-level links externally instead of granting them Nova WebView context;
- disables cleartext traffic, WebView file access, content access, file-URL cross-origin access and mixed content;
- does not register a JavaScript bridge;
- keeps WebView debugging limited to debug builds;
- supports the existing HTML image input through a native chooser and optional camera capture using a scoped `FileProvider`; and
- does not request direct camera permission because capture is delegated to an installed camera activity.

## Build status

The source/config contract is covered by `nova-next/tests/android-wrapper-contract.test.mjs`. A full Android Gradle build has not been claimed in the current execution container because it does not provide the Gradle wrapper/executable for this standalone project. The project should be built in an Android/CI environment before an APK is considered verified.

## Promotion

Replacing the installed production Nova later requires an explicit promotion release that switches to the existing `com.buysloans.nova` identity, uses the existing production signing key, preserves Android upgrade-compatible version ordering and passes the documented promotion/security gates. None of those production identity/signing steps are active in this development wrapper.
