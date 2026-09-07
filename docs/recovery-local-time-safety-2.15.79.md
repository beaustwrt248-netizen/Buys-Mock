# Safety property

The formatter uses `runCatching` and returns the original nonblank value on parse failure. A backend format change can therefore reduce presentation quality but cannot prevent the user from opening Backup & Data or operating existing backup controls.
