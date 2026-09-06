# Verification evidence

Affected surfaces: main Morley Android More menu, main Android manifest and embedded-admin implementation, and main Morley web bootstrap. Dedicated `admin/` website and `android/adminapp/` remain present.

Unaffected by design: pricing engines and approvals, catalogue data, Guardian/Nova approval boundaries, server-side admin authorization, RLS, privileged backend APIs, inventory, sales and support data.

Responsive impact: removal only; no new controls, widths, overlays or navigation destinations were introduced. Existing responsive shells remain unchanged.

Regression coverage: Android source-contract tests plus a cross-platform separation workflow verify that main Morley cannot reintroduce embedded Admin Mode while the dedicated Admin products remain present.
