# Morley Owner-Optional Automation Policy

Morley automation should complete normal engineering, review, merge, build, deployment, release, monitoring, retry, and recovery work without requiring routine owner interaction.

## Autonomous lanes

Routine and guarded-sensitive changes may proceed automatically when repository-required checks pass. This includes UI, catalogue, application logic, Admin, Nova, Guardian implementation, workflow maintenance, migrations, auth-related implementation, releases, deployments, parity work, regression repairs, and operational remediation that preserves existing trust and data-safety invariants.

Guarded-sensitive does not mean unreviewed. It means automated policy review plus the repository's security, quality, parity, Guardian, release, OTA and integration checks are the approval evidence.

## Critical autonomy stops

Automation must fail closed when a change can irreversibly alter trust or production data, including:

- credential, private-key, keystore or signing-secret material;
- disabling row-level security;
- broad anonymous/authenticated `GRANT ALL` privilege expansion;
- service-role material in client/repository changes;
- destructive `DROP TABLE`, `DROP SCHEMA`, `DROP DATABASE`, `TRUNCATE`, or equivalent unbounded destructive production operations;
- explicit Guardian authority expansion or exposure of protected raw evidence, prices, or backup material;
- equivalent changes detected by future policy checks.

These stops are exceptional escape hatches, not normal approval gates.

## Operating principle

The owner should normally receive outcomes and incidents, not approval chores. Automation retries transient failures, escalates persistent failures, and recovers/continues automatically whenever safe evidence exists. Human intervention is reserved for critical-stop decisions or external account/credential actions that cannot be delegated safely.
