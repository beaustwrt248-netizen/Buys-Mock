import fs from 'node:fs';
import assert from 'node:assert/strict';
import test from 'node:test';

const orchestrator = fs.readFileSync('supabase/functions/nova-orchestrator/index.ts', 'utf8');
const metrics = fs.readFileSync('supabase/functions/nova-ai-metrics/index.ts', 'utf8');
const vision = fs.readFileSync('supabase/functions/nova-vision/index.ts', 'utf8');
const migration = fs.readFileSync('supabase/migrations/20260911185000_nova_ai_runs.sql', 'utf8');
const apiClient = fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaApiClient.java', 'utf8');
const operatorUi = fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaOperatorUi.java', 'utf8');
const damageReview = fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaDamageReviewActivity.java', 'utf8');
const reviewBridge = fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionReviewBridge.java', 'utf8');
const functionalityGate = fs.readFileSync('android/novaapp/src/main/java/com/buysloans/nova/NovaVisionFunctionalityGate.java', 'utf8');
const manifest = fs.readFileSync('android/novaapp/src/main/AndroidManifest.xml', 'utf8');

test('orchestrator supports explicit provider routing and consensus', () => {
  for (const provider of ['gpt', 'gemini', 'claude', 'consensus']) assert.match(orchestrator, new RegExp(`['\"]${provider}['\"]`));
  assert.match(orchestrator, /PROVIDER_MODELS/);
  assert.match(orchestrator, /provider === 'consensus'/);
  assert.match(orchestrator, /body\.provider/);
});

test('orchestrator bounds spend and handles transient provider failures', () => {
  assert.match(orchestrator, /NOVA_MAX_REQUEST_COST_USD/);
  assert.match(orchestrator, /ensemble-unfused-budget/);
  assert.match(orchestrator, /for \(let attempt = 0; attempt < 2; attempt\+\+\)/);
  assert.match(orchestrator, /CIRCUIT_OPEN/);
  assert.match(orchestrator, /openUntil: shouldOpen \? Date\.now\(\) \+ 60_000/);
});

test('consensus checks measured candidate spend before starting the fusion call', () => {
  const candidateUsage = orchestrator.indexOf('const candidateUsage = usageTotals(successes)');
  const budgetCheck = orchestrator.indexOf('candidateUsage.cost_usd >= MAX_REQUEST_COST_USD');
  const budgetFallback = orchestrator.indexOf("mode: 'ensemble-unfused-budget'");
  const fusionCall = orchestrator.indexOf('const final = await fuse(prompt, successes)');
  assert.ok(candidateUsage >= 0, 'candidate usage accounting must remain present');
  assert.ok(budgetCheck > candidateUsage, 'measured candidate cost must be checked after accounting');
  assert.ok(budgetFallback > budgetCheck, 'over-budget consensus must preserve the unfused fallback');
  assert.ok(fusionCall > budgetFallback, 'fusion must not start until after the measured-cost guardrail');
});

test('telemetry stores operational metadata but not prompts or model responses', () => {
  assert.match(orchestrator, /admin\.from\('nova_ai_runs'\)\.insert/);
  assert.match(migration, /create table if not exists public\.nova_ai_runs/);
  assert.match(migration, /latency_ms integer/);
  assert.match(migration, /cost_usd numeric/);
  assert.doesNotMatch(migration, /\bprompt\b/i);
  assert.doesNotMatch(migration, /\bresponse\b/i);
  assert.doesNotMatch(migration, /\banswer\b/i);
});

test('telemetry remains service-side and admin metrics are JWT-gated', () => {
  assert.match(migration, /enable row level security/);
  assert.match(migration, /revoke all on table public\.nova_ai_runs from anon, authenticated/);
  assert.match(metrics, /admin\.auth\.getUser/);
  assert.match(metrics, /profile\?\.is_enabled && profile\.role === 'admin'/);
  assert.match(metrics, /prompts and model responses are not stored/i);
});

test('protected Nova authority remains advisory and human gated', () => {
  assert.match(orchestrator, /You are advisory in this endpoint/);
  assert.match(orchestrator, /Protected actions remain human-gated/);
  assert.match(orchestrator, /never claim you executed catalogue writes, pricing changes, Guardian decisions\/repairs, deployments, releases, OTA actions, role\/user changes, destructive deletes, support sends, or GitHub merges\/releases/);
});

test('native client can choose providers, retain last run metadata and read AI metrics', () => {
  assert.match(apiClient, /JSONObject orchestrate\(String prompt, String mode, String provider\)/);
  assert.match(apiClient, /\.put\("provider", requestedProvider\)/);
  assert.match(apiClient, /setPreferredProvider\(String provider\)/);
  assert.match(apiClient, /lastOrchestratorResult = result/);
  assert.match(apiClient, /JSONObject aiMetrics\(\)/);
  assert.match(apiClient, /edge\("nova-ai-metrics"/);
});

test('native chat exposes selectable AI modes and safe run metadata', () => {
  for (const label of ['Auto', 'GPT', 'Gemini', 'Claude', 'Consensus']) assert.match(operatorUi, new RegExp(`\"${label}\"`));
  assert.match(operatorUi, /getSharedPreferences\("nova_ai"/);
  assert.match(operatorUi, /Last AI run:/);
  assert.match(operatorUi, /latency_ms/);
  assert.match(operatorUi, /cost_usd/);
  assert.match(operatorUi, /fallback/);
});

test('voice input uses runtime microphone permission, speech recognition and hands-free send', () => {
  assert.match(manifest, /android\.permission\.RECORD_AUDIO/);
  assert.match(operatorUi, /SpeechRecognizer\.createSpeechRecognizer/);
  assert.match(operatorUi, /RecognizerIntent\.ACTION_RECOGNIZE_SPEECH/);
  assert.match(operatorUi, /partialResults/);
  assert.match(operatorUi, /sendVoiceResult/);
  assert.match(operatorUi, /getDeclaredMethod\("sendQuestion"/);
});

test('Vision localises only validated visible damage regions for overlays', () => {
  assert.match(vision, /damage_regions/);
  assert.match(vision, /normaliseDamageRegions/);
  assert.match(vision, /center_x/);
  assert.match(vision, /center_y/);
  assert.match(vision, /radius/);
  assert.match(vision, /Omit uncertain or merely inferred regions rather than guessing coordinates/);
  assert.match(vision, /Math\.max\(\.02,Math\.min\(\.45,rawRadius\)\)/);
});

test('native Vision damage review circles regions without changing pricing authority', () => {
  assert.match(manifest, /NovaDamageReviewActivity/);
  assert.match(reviewBridge, /sessionUris/);
  assert.match(reviewBridge, /lastAssessment/);
  assert.match(damageReview, /canvas\.drawCircle/);
  assert.match(damageReview, /staff must physically inspect every device/i);
  assert.match(functionalityGate, /Review highlighted damage/);
  assert.match(functionalityGate, /manual pricing/i);
  assert.match(functionalityGate, /human final approval/i);
});
