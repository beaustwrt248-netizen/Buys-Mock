# Main Morley admin-mode removal

The main B&L Morley website and Android app no longer expose or bundle the redundant embedded Admin Mode. Administrative control remains in the dedicated Morley Admin website under `admin/` and the dedicated Android Admin app under `android/adminapp/`.

This change does not remove server-side role enforcement, privileged admin APIs, RLS, pricing approvals, Guardian/Nova human-approval boundaries, or the dedicated Morley Admin products.

Verification is run against the current Morley OTA baseline, with 2.15.72 promoted before the 2.15.73 admin-mode-removal release is merged.
