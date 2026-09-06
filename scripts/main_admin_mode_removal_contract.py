#!/usr/bin/env python3
from pathlib import Path

root = Path(__file__).resolve().parents[1]
index = (root / "index.html").read_text(encoding="utf-8")
admin_index = (root / "admin" / "index.html").read_text(encoding="utf-8")
manifest = (root / "android" / "app" / "src" / "main" / "AndroidManifest.xml").read_text(encoding="utf-8")
dashboard = (root / "android" / "app" / "src" / "main" / "java" / "com" / "buysloans" / "hub" / "DashboardActivity.kt").read_text(encoding="utf-8")

assert "web-admin-mode.js" not in index, "Main website must not load the retired admin-mode injector"
assert not (root / "web-admin-mode.js").exists(), "Retired web admin-mode injector must stay deleted"
assert "EmbeddedAdminActivity" not in manifest, "Main Android manifest must not expose embedded admin mode"
assert "Admin mode" not in dashboard, "Main Android menu must not expose admin mode"
assert "B&L Morley Admin" in admin_index, "Dedicated Morley Admin website must remain intact"
assert (root / "android" / "adminapp").is_dir(), "Dedicated Morley Admin Android module must remain intact"

print("Main Morley admin-mode removal contract passed")
