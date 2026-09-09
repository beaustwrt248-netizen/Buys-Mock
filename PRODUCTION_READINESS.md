# Morley Production Readiness

This checklist tracks the finalisation pass for the Morley Android app and Admin Control lanes. Workstreams remain isolated unless explicitly authorised.

A checked item means the current repository/live-control evidence is sufficient to verify the control. Physical-device, production cutover, destructive restore, or full end-to-end acceptance items remain unchecked until they are actually exercised.

## Main Morley Android app
- [ ] Full seller -> ask/buy -> valuation -> buy-complete regression coverage
- [ ] Real-device Samsung smoke pass documented
- [ ] Report-a-problem/support flow regression pass
- [x] OTA/update behavior regression pass
- [ ] Crash/health telemetry verification

## Admin Control
- [x] Support-ticket SLA controls
- [x] Protected-message access verification
- [x] User-management permission verification
- [x] Device/app-version visibility completion
- [x] Safe remote configuration guardrails
- [x] Durable audit coverage
- [ ] Real-device login/CAPTCHA regression pass

## Security / release hardening
- [ ] GitHub Actions least-privilege review
- [x] Supabase SECURITY DEFINER/service-role review
- [x] Storage-policy review
- [x] Signing/checksum safeguards review
- [ ] Private-distribution readiness review
- [ ] Controlled dependency compatibility batches

## Guardian
- [x] Health telemetry coverage
- [x] Safe automated remediation boundaries
- [x] Human approval preserved for powerful/code-changing actions
- [x] Audit/history coverage

## Backup / recovery
- [x] Google Drive backup upload integrity/read-back verification
- [x] Per-user AES-256-GCM backup integrity and ownership checks
- [x] Pre-restore safety backup and write-before-prune safeguards
- [ ] Non-destructive restore rehearsal documented

## Release exit criteria
- [ ] Required CI green on exact release SHA
- [ ] Installable APK artifact verified
- [ ] OTA manifest matches release artifact and digest
- [ ] No open production-blocking defects
- [ ] Final Samsung real-device acceptance pass

## Evidence notes — 9 September 2026

- Admin support governance contract verifies ticket ownership/RLS, assignment, SLA due timestamps, first-response tracking, and priority windows of urgent 2h, high 8h, normal 24h.
- Live support RLS verification confirms customer message access is limited to the ticket owner; Admin/Manager can access governed tickets; support staff access is limited to tickets assigned to them. Internal notes are restricted to Admin/Manager or assigned support staff and are not exposed to customers. Attachment access follows the same owner/Admin/assigned-staff boundary.
- Admin integration contract verifies user-control wiring, device/app-version visibility, support controls, audit access, Android Admin web parity, automatic OTA scheduling, OTA feature governance, trusted release URLs and downloaded APK SHA-256 validation.
- Remote configuration is restricted to maintenance mode/message and OTA enablement and requires Admin/Manager authority; the private implementation is not directly executable by normal authenticated clients.
- Privileged support-ticket changes are durably audited while ticket/message body content is excluded from the audit record.
- Supabase live review verified public-table RLS, client TRUNCATE revocation, SECURITY DEFINER/service-role boundaries, and restricted private support attachment storage.
- Guardian platform contracts verify missing telemetry cannot be reported healthy, sensitive readiness is aggregated for Admin/Manager, raw sensitive datasets are not exposed, automated authority is not expanded, and final merge remains human-controlled.
- Global Drive backup verifies the uploaded file by downloading it and recomputing SHA-256. Per-user Drive backup validates ciphertext SHA-256, AES-256-GCM decryption, Morley user ownership and Google-account identity. Restore creates a safety backup before writes and the merged write-before-prune flow avoids clearing live valuation history before restored rows are accepted.
- Private-distribution tooling is prepared but deliberately not marked complete while public-resource dependencies still block private-origin cutover.
- Physical Samsung smoke/acceptance and real-device CAPTCHA remain external-device acceptance items.
