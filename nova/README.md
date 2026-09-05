# Nova AI standalone control centre

This directory is the isolated Nova application surface. It must not depend on Morley Admin UI state or inherit Admin write authority.

## Phase 1

- Standalone responsive/PWA-style shell.
- Public read-only GitHub status for `beaustwrt248-netizen/Buys-Mock`.
- Nova pull-request queue and high-risk attention surface.
- No production writes.
- No pricing changes.
- No authentication or permission mutation.
- No Guardian repair execution.
- No release/deployment authority.

## Planned boundaries

Nova-specific authentication and backend capabilities will be introduced separately. Morley Admin remains the operational business administration application. Guardian remains an independent validation layer. Protected actions must continue through explicit human approval paths.

Deployment workflow changes are intentionally excluded from this phase because GitHub Actions/release controls are high-risk governance areas.
