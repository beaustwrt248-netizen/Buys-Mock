# Morley Admin Native Rebuild — Red Phase Evidence

The rebuild starts with regression contracts before implementation. The current production architecture is intentionally expected to fail these tests because `AdminActivity` is still a WebView workspace and native login still transfers tokens through intent extras.

Expected initial failures:
- `AdminSessionStoreTest`: `AdminSessionStore` does not exist yet.
- `AdminNativeWorkspaceContractTest`: current `AdminActivity` contains WebView/session injection and no native session store.
- `AdminNativeLoginNavigationContractTest`: current login passes access/refresh tokens via intent extras instead of installing a native process session first.

These tests are committed before implementation so the PR CI run provides external RED evidence before the architecture is replaced.
