# Nova Next

Nova Next is a from-scratch, isolated successor candidate for Nova AI. It implements the approved dark blue/purple reference layout while keeping the existing `nova/` app untouched.

## Bootstrap scope

This first branch establishes the new UI shell, navigation, feature-parity registry, Admin-only auth policy contract, fail-closed action policy and promotion-ready identity separation. Live production auth/backend wiring is intentionally not activated in this bootstrap because those are protected/high-risk boundaries that require their own reviewed change.

## Local contract tests

```bash
for f in nova-next/tests/*.test.mjs; do node "$f"; done
```

## Isolation rule

Nova Next must not import frontend/runtime code from `../nova/`. Shared backend behavior is reached only through reviewed adapter contracts and existing protected server-side authorization.
