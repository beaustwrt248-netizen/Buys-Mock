# CI note

The deterministic test does not rely on the runner's local zone. The broader Android PR workflow remains responsible for compilation, regression tests, lint and APK build; repository security and UI/feature parity workflows provide the protected integration gates.
