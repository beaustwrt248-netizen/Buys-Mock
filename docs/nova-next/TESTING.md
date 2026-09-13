# Nova Next Testing

Run all dependency-free bootstrap contract tests with:

```bash
for f in nova-next/tests/*.test.mjs; do node "$f"; done
```

The current bootstrap suite covers navigation, routing, Admin-only authorization policy, auth adapter behavior, capability parity registry, fail-closed action policy, API transport gating, promotion-channel separation and source isolation.
