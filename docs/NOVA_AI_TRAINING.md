# Nova AI training model

Nova AI learns through bounded, inspectable experience rather than hidden self-modification.

## Persistent experience

`nova_learning_experiences` stores source-attributed lessons derived from verified operational outcomes or explicit Admin/Manager feedback.

Each experience records:
- domain and lesson type
- stable lesson key
- source type and source identifier
- human-readable summary
- structured evidence
- outcome and confidence
- verified/non-verified status
- observation and creation timestamps

The table is not directly available to `anon` or `authenticated` clients. Access is through the JWT-authenticated `nova-learning` Edge Function, which independently verifies the caller is an enabled Admin or Manager.

## Training pass

A training pass (`action=harvest`) deterministically harvests lessons from authoritative source tables:

1. Guardian incidents that are verified/resolved or demonstrably recurring.
2. Guardian repairs with completed/verified/applied outcomes or failed/rejected/quarantined outcomes.
3. Valuations with both expected and realised profit.

Lessons are deduplicated with stable domain/lesson keys and can be recomputed from source data.

## Human feedback

Explicit Admin/Manager feedback can be stored as non-authoritative context. It starts unverified and at lower confidence than verified source outcomes. Feedback never rewrites the underlying source-of-truth record.

## Hard boundaries

Learning may improve diagnosis, ranking, explanations, confidence and future proposals. It must never:
- lower a capability risk class
- bypass human approval
- grant roles or permissions
- alter RLS, auth, secrets or deployment authority
- execute Guardian protected repairs
- silently mutate catalogue, pricing, valuation, support, inventory or release state
- override newer verified source data

Guardian remains Nova's protected diagnostics and repair subsystem. Nova may learn from Guardian but does not inherit unrestricted Guardian execution authority.

## Next training domains

After authoritative source contracts are verified, the same experience model can expand to catalogue corrections, inventory lifecycle, sales/realised margin, support resolution quality and release/regression outcomes.
