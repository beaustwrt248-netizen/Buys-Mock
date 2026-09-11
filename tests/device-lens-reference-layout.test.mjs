import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const dashboard = fs.readFileSync("android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt", "utf8");
const scan = fs.readFileSync("android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt", "utf8");

test("Morley home follows the approved scan-first reference layout", () => {
  for (const label of [
    "Search devices, stock or scan...",
    "Scan Device",
    "Manual Search",
    "Add to Catalogue",
    "Price Check",
    "View Stock",
    "Settings",
    "Google camera search",
  ]) assert.match(dashboard, new RegExp(label.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));

  for (const nav of ["Home", "Stock", "Scan", "Trade", "More"]) {
    assert.match(dashboard, new RegExp(`\\b${nav}\\b`));
  }
});

test("two-photo capture mirrors the reference controls", () => {
  assert.match(scan, /Take Photos/);
  assert.match(scan, /Photo 1 of 2/);
  assert.match(scan, /Photo 2 of 2/);
  assert.match(scan, /Gallery/);
  assert.match(scan, /Flash/);
  assert.match(scan, /CameraCorners/);
  assert.match(scan, /LinearProgressIndicator/);
  assert.match(scan, /Analysing visual details/);
});

test("results retain damage circles, condition, pricing and stock flow", () => {
  assert.match(scan, /drawOval/);
  assert.match(scan, /Damage Detected/);
  assert.match(scan, /View Damage Close-up/);
  assert.match(scan, /Condition Summary/);
  assert.match(scan, /Live Price Check/);
  assert.match(scan, /Use Suggested Sell Price/);
  assert.match(scan, /Add to Catalogue/);
  assert.match(scan, /View in Stock/);
});

test("Google branding is reserved for the real code scanner, not Morley Vision analysis", () => {
  assert.match(dashboard, /Google Play services is used separately for code scanning/);
  assert.match(scan, /Morley Vision is analysing both photos/);
  assert.doesNotMatch(scan, /Google Lens/);
  assert.match(scan, /AI-powered visual estimate/);
});
