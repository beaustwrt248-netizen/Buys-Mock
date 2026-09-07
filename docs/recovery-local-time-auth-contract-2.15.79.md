# Morley authentication contract unchanged

`AuthManager.validAccessToken(context)` remains the source of the Morley bearer token for backup API requests. The timestamp formatter executes only after successful authenticated responses and does not read or persist authentication state.
