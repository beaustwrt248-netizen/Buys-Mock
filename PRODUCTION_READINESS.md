# Morley Production Readiness

This checklist tracks the finalisation pass for the Morley Android app and Admin Control lanes. Workstreams remain isolated unless explicitly authorised.

## Main Morley Android app
- [ ] Full seller -> ask/buy -> valuation -> buy-complete regression coverage
- [ ] Real-device Samsung smoke pass documented
- [ ] Report-a-problem/support flow regression pass
- [ ] OTA/update behavior regression pass
- [ ] Crash/health telemetry verification

## Admin Control
- [ ] Support-ticket SLA controls
- [ ] Protected-message access verification
- [ ] User-management permission verification
- [ ] Device/app-version visibility completion
- [ ] Safe remote configuration guardrails
- [ ] Durable audit coverage
- [ ] Real-device login/CAPTCHA regression pass

## Security / release hardening
- [ ] GitHub Actions least-privilege review
- [ ] Supabase SECURITY DEFINER/service-role review
- [ ] Storage-policy review
- [ ] Signing/checksum safeguards review
- [ ] Private-distribution readiness review
- [ ] Controlled dependency compatibility batches

## Guardian
- [ ] Health telemetry coverage
- [ ] Safe automated remediation boundaries
- [ ] Human approval preserved for powerful/code-changing actions
- [ ] Audit/history coverage

## Release exit criteria
- [ ] Required CI green on exact release SHA
- [ ] Installable APK artifact verified
- [ ] OTA manifest matches release artifact and digest
- [ ] No open production-blocking defects
- [ ] Final Samsung real-device acceptance pass
