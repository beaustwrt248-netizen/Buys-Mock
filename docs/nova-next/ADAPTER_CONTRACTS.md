# Nova Next Adapter Contracts

Nova Next talks to approved backend services only through narrow adapters. The UI must not access protected production tables directly.

## Authentication

`createAuthAdapter()` receives injected implementations for human verification, password exchange, profile loading and remote sign-out. Entry succeeds only when the authenticated profile is enabled and has role `admin`. Production wiring must preserve Cloudflare Turnstile and the current server-side role checks.

## Nova API

`createNovaApi()` receives an authenticated token provider and a transport. Calls return the normalized shape:

```js
{
  ok: boolean,
  data: unknown,
  evidence: Array,
  error: null | { code: string, message: string },
  policy: { risk: string, protected: boolean, auto: boolean }
}
```

Only actions explicitly allow-listed by `action-policy.mjs` may reach the transport automatically. Unknown actions fail closed.

## Protected operations

The client must not auto-execute pricing writes, Guardian repair approval/execution, deployment, OTA publication, signing changes, role/user changes, destructive deletes or catalogue patch application. Those actions remain in their existing human approval paths.

## Evidence

Research, catalogue, pricing, support, Guardian, release and business-intelligence adapters should return evidence/provenance alongside material claims. Missing evidence is a failure state for workflows that require evidence; it must not be interpreted as permission or confidence.
