# Nova Next Identity

Nova Next development identity is deliberately separate from production Nova.

- Display name: `Nova Next`
- Development Android application ID target: `com.buysloans.novanext`
- Development PWA scope: `/nova-next/`
- Development cache namespace: `nova-next-dev-v1`
- Current production Nova Android application ID: `com.buysloans.nova`
- Current production Nova web scope: `/nova/`

The production values above were read from the current Nova Android/Web configuration rather than guessed. Recording them does not activate them in Nova Next. Nova Next keeps its distinct development identity until an explicit future promotion; production package/signing/routing activation remains a protected release step.
