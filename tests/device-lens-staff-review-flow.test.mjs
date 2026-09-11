import fs from 'node:fs';
import assert from 'node:assert/strict';

// Live-flow contract: staff verification must remain actionable and fail closed before pricing/stock.
const source = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt', 'utf8');
const review = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyVisionReviewUi.kt', 'utf8');
const inspection = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/DeviceInspectionClient.kt', 'utf8');
const policy = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyVisionPolicy.kt', 'utf8');

assert.match(source, /ANALYSING,\s*REVIEW,\s*RESULTS/, 'Device Lens must have a dedicated staff REVIEW step');
assert.match(source, /reviewState\s+by\s+remember\s*\{\s*mutableStateOf<MorleyVisionReviewState\?>\(null\)\s*\}/, 'Device Lens must retain Morley Vision review state');
assert.match(source, /reviewState\s*=\s*MorleyVisionReviewPolicy\.from\(it, null\)[\s\S]*?step\s*=\s*LensStep\.REVIEW/, 'analysis must enter staff review before results');
assert.match(source, /MorleyVisionReviewPanel\(/, 'live Device Lens must render the reusable Morley Vision review panel');
assert.match(source, /MorleyVisionReviewPolicy\.decideDamage\(/, 'damage Confirm\/Not Damage decisions must be applied through the review policy');
assert.match(source, /enabled\s*=\s*state\.canCompleteStaffReview/, 'continue action must stay disabled until staff review is complete');
assert.match(source, /fun requireStaffReview\(\): Boolean[\s\S]*?reviewState\?\.canCompleteStaffReview/, 'pricing and stock transitions must use the staff-review gate');
assert.match(source, /fun openPricing\(\)[\s\S]*?if \(!requireStaffReview\(\)\) return/, 'live pricing must fail closed when review is incomplete');
assert.match(source, /addStock\s*=\s*\{ if \(requireStaffReview\(\)\) step = LensStep\.ADD_STOCK \}/, 'results must not enter stock flow before review completion');
assert.match(source, /reviewState\s*=\s*null[\s\S]*?step\s*=\s*LensStep\.CAPTURE_FRONT/, 'retake/reset must clear prior staff-review state');

assert.match(review, /Confirm storage/, 'review must expose an explicit storage confirmation path when AI storage is unverified');
assert.match(review, /Only confirm a value you can verify[\s\S]*Do not guess/, 'storage confirmation must explicitly prohibit guessing');
assert.match(review, /staffStoragePattern[\s\S]*GB\|TB/, 'manual staff storage must be validated as a storage quantity');
assert.match(review, /fun confirmStorage\([\s\S]*state\.inspection\.storage\s*=\s*verified[\s\S]*storageVerifiedState\.value\s*=\s*true/, 'staff confirmation must update the same inspection used by live pricing and unlock review only after validation');
assert.match(review, /canCompleteStaffReview[\s\S]*identityVerified\s*&&\s*storageVerified/, 'review must remain fail-closed until storage is verified');
assert.match(inspection, /var storage:\s*String/, 'inspection storage must support session-local explicit staff verification');
assert.match(inspection, /identityQuery[\s\S]*verifiedStorage/, 'live pricing identity must include the now staff-verified storage value');
assert.match(policy, /expectedStorage = canonicalStorage\(clean\(inspection\.storage\)\)/, 'market comparables must continue rejecting explicit storage mismatches');
assert.doesNotMatch(source + review + inspection + policy, /Gumtree/i, 'Device Lens staff review integration must not add Gumtree behavior');

console.log('Device Lens staff-review and storage-verification integration contract passed.');
