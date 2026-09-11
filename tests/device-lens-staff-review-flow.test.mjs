import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt', 'utf8');

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
assert.doesNotMatch(source, /Gumtree/i, 'Device Lens staff review integration must not add Gumtree behavior');

console.log('Device Lens staff-review integration contract passed.');
