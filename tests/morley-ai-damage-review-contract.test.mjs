import fs from 'node:fs';
import assert from 'node:assert/strict';

const activity = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt', 'utf8');
const bridge = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyAssessmentBridge.kt', 'utf8');
const review = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyVisionReviewUi.kt', 'utf8');

assert.match(activity, /private fun AnnotatedPhoto\([\s\S]*region\.x\.coerceIn\(0f, 1f\)[\s\S]*region\.y\.coerceIn\(0f, 1f\)/, 'damage overlay must clamp normalized coordinates');
assert.match(activity, /ContentScale\.Fit[\s\S]*offsetX[\s\S]*offsetY/, 'damage overlay must account for fitted-image letterboxing');
assert.match(activity, /drawOval\([\s\S]*LensDanger/, 'damage findings must remain visibly circled on the photo');
assert.match(activity, /inspection\.damageRegions\.filter \{ it\.photoIndex == 1 \}[\s\S]*inspection\.damageRegions\.filter \{ it\.photoIndex == 2 \}/, 'front and back damage regions must stay mapped to their source photos');

assert.match(bridge, /val confirmedDamageCount: Int/, 'assessment snapshot must expose staff-confirmed damage count');
assert.match(bridge, /val dismissedDamageCount: Int/, 'assessment snapshot must expose staff-dismissed false-positive count');
assert.match(bridge, /val pendingDamageCount: Int/, 'assessment snapshot must expose pending damage review count');
assert.match(bridge, /confirmedDamageCount\s*=\s*review\.damageReviews\.count \{ it\.decision == VisionStaffDecision\.CONFIRMED \}/, 'confirmed damage must derive from staff review decisions');
assert.match(bridge, /dismissedDamageCount\s*=\s*review\.damageReviews\.count \{ it\.decision == VisionStaffDecision\.NOT_DAMAGE \}/, 'dismissed damage must remain auditable');
assert.match(bridge, /pendingDamageCount\s*=\s*review\.unresolvedDamageCount/, 'pending damage must preserve the fail-closed gate');
assert.match(review, /confirmedDamageCount[\s\S]*dismissedDamageCount[\s\S]*pendingDamageCount/, 'staff assessment UI must summarise damage review decisions');
assert.doesNotMatch(activity + bridge + review, /auto[- ]?reject/i, 'damage AI must not automatically reject a device');

console.log('Morley AI damage mapping and staff-review contract passed.');
