# Nova Next Identity

Nova Next development identity is deliberately separate from production Nova.

- Display name: `Nova Next`
- Development Android application ID target: `au.com.morley.novanext.dev`
- Development PWA scope: `/nova-next/`
- Development cache namespace: `nova-next-dev-v1`

Production Nova package/scope values are not copied into the bootstrap by guesswork. They must be read from the current signed/released Nova configuration and recorded only as part of the promotion-readiness review. Production activation requires explicit promotion approval.
