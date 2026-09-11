import fs from 'node:fs';
import assert from 'node:assert/strict';

const activity = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/DeviceLensActivity.kt', 'utf8');
const review = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyVisionReviewUi.kt', 'utf8');
const bridge = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyAssessmentBridge.kt', 'utf8');
const core = fs.readFileSync('morley-ai-assessment-core.js', 'utf8');

assert.match(bridge, /data class MorleyAssessmentSnapshot/, 'Android must expose a typed assessment snapshot');
assert.match(bridge, /fun from\([\s\S]*DeviceInspection[\s\S]*MorleyVisionReviewState/, 'assessment snapshot must be derived from the live Device Lens inspection and staff review state');
assert.match(bridge, /identityResolved\s*=\s*inspection\.identityVerified/, 'assessment identity must use the existing verified Device Lens identity');
assert.match(bridge, /storageResolved\s*=\s*review\.storageVerified/, 'assessment storage must use the existing staff-verified storage gate');
assert.match(bridge, /evidenceSufficient\s*=/, 'assessment must explicitly represent evidence sufficiency');
assert.match(bridge, /valuationBlockedReason/, 'assessment must carry an explicit valuation blocker reason');
assert.match(bridge, /commercialAuthority\s*=\s*"advisory"/, 'AI commercial output must remain advisory');
assert.match(bridge, /requiresStaffConfirmation\s*=\s*true/, 'assessment must always require staff confirmation');
assert.match(bridge, /when \{[\s\S]*!identityResolved[\s\S]*"identity_unresolved"[\s\S]*!storageResolved[\s\S]*"storage_unresolved"[\s\S]*!evidenceSufficient[\s\S]*"evidence_insufficient"/, 'Android assessment states must preserve the shared fail-closed ordering');

assert.match(activity, /assessmentSnapshot\s+by\s+remember\s*\{\s*mutableStateOf<MorleyAssessmentSnapshot\?>\(null\)\s*\}/, 'Device Lens must retain assessment state alongside review state');
assert.match(activity, /assessmentSnapshot\s*=\s*MorleyAssessmentBridge\.from\(it, reviewState!!, null\)/, 'analysis must build an assessment from the verified inspection/review contract');
assert.match(activity, /assessmentSnapshot\s*=\s*MorleyAssessmentBridge\.from\(current, updatedReview, it\)/, 'pricing evidence must refresh the assessment without bypassing review');
assert.match(activity, /assessmentSnapshot\s*=\s*null[\s\S]*reviewState\s*=\s*null/, 'reset/retake must clear assessment state with review state');
assert.match(activity, /MorleyAssessmentStatusCard\(/, 'Device Lens review must render assessment status to staff');

assert.match(review, /fun MorleyAssessmentStatusCard\(/, 'staff review UI must expose the assessment status card');
assert.match(review, /Review required|Blocked|Verified/, 'assessment status must be explained in text, not colour alone');
assert.match(review, /valuationBlockedReason/, 'staff UI must show why valuation is blocked');

assert.match(core, /commercialAuthority:\s*'advisory'/, 'browser/shared assessment contract must remain advisory');
assert.match(core, /requiresStaffConfirmation:\s*true/, 'browser/shared assessment contract must retain staff confirmation');
assert.doesNotMatch(activity + review + bridge, /Gumtree/i, 'assessment integration must not add Gumtree behavior');

console.log('Morley AI Device Lens assessment integration contract passed.');
