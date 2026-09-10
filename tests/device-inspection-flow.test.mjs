import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const manifest = fs.readFileSync("android/app/src/main/AndroidManifest.xml", "utf8");
const gradle = fs.readFileSync("android/app/build.gradle", "utf8");
const dashboard = fs.readFileSync("android/app/src/main/java/com/buysloans/hub/DashboardActivity.kt", "utf8");
const activity = fs.readFileSync("android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt", "utf8");
const client = fs.readFileSync("android/app/src/main/java/com/buysloans/hub/DeviceInspectionClient.kt", "utf8");
const edge = fs.readFileSync("supabase/functions/device-inspection/index.ts", "utf8");

test("main app exposes the two-photo device scan from the dashboard", () => {
  assert.match(dashboard, /Scan Device/);
  assert.match(dashboard, /DeviceLensActivity::class\.java/);
  assert.match(manifest, /android\.permission\.CAMERA/);
  assert.match(manifest, /\.DeviceLensActivity/);
});

test("capture flow requires a front and back photo and uses CameraX", () => {
  assert.match(activity, /Photo 1 of 2/);
  assert.match(activity, /Photo 2 of 2/);
  assert.match(activity, /Take a clear photo of the front/);
  assert.match(activity, /Take a clear photo of the back/);
  assert.match(activity, /ImageCapture/);
  assert.match(activity, /PreviewView/);
  assert.match(client, /REQUIRED_PHOTOS = 2/);
  assert.match(gradle, /androidx\.camera:camera-camera2:1\.6\.2/);
  assert.match(gradle, /androidx\.camera:camera-lifecycle:1\.6\.2/);
  assert.match(gradle, /androidx\.camera:camera-view:1\.6\.2/);
});

test("damage detections are normalized and rendered as red circles", () => {
  assert.match(edge, /damage_regions/);
  assert.match(edge, /photo_index/);
  assert.match(edge, /x and y are the top-left/);
  assert.match(edge, /Do not create a region for reflections, glare, fingerprints/);
  assert.match(activity, /drawOval/);
  assert.match(activity, /LensDanger/);
  assert.match(activity, /Damage Detected/);
  assert.match(activity, /AI-powered visual estimate/);
});

test("inspection privacy and auth boundaries remain explicit", () => {
  assert.match(edge, /\.select\("is_enabled"\)/);
  assert.match(edge, /profile\?\.is_enabled/);
  assert.match(edge, /images\.length !== REQUIRED_IMAGES/);
  assert.match(edge, /stored: false/);
  assert.match(edge, /full_serials_returned: false/);
  assert.doesNotMatch(edge, /\.from\("storage\.objects"\)/);
  assert.match(client, /AuthManager\.validAccessToken/);
});

test("identified devices reuse catalogue, pricing and stock functionality", () => {
  assert.match(edge, /\.from\("device_catalog"\)/);
  assert.match(client, /market-search-v2/);
  assert.match(client, /ConditionAdjustment\.assess/);
  assert.match(activity, /WorkspaceStore\.addInventory/);
  assert.match(activity, /Use Suggested Sell Price/);
  assert.match(activity, /View in Stock/);
});

test("device inspection pricing excludes Gumtree", () => {
  assert.doesNotMatch(client, /gumtree/i);
});
