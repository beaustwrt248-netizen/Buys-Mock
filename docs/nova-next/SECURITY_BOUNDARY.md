# Nova Next Security Boundary

Nova Next is an isolated successor candidate. It must not weaken the current Nova or Guardian security model.

- Current Nova remains untouched while Nova Next is developed.
- Nova Next is Admin-only when connected to production identity services.
- A successful password exchange is not enough: access requires an enabled Admin profile.
- Public/publishable client credentials are allowed; service-role, AI provider, signing and other privileged secrets are never shipped in browser or APK source.
- Protected operations remain server-authorized and audited.
- Pricing approval/write, Guardian approval/repair execution, release/deployment/OTA/signing, role/user changes, destructive deletes and silent catalogue patch application remain outside Nova Next autonomous authority.
- Unknown or incompletely classified protected actions fail closed.
- Guardian remains independently authoritative and cannot be disabled, bypassed, weakened or self-approved by Nova Next.
- The bootstrap login flow is visual-preview-only until the production auth adapter is explicitly connected and reviewed.
