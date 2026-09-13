export const SAFE_CLIENT_FUNCTIONS = Object.freeze([
  'nova-orchestrator',
  'nova-knowledge',
  'nova-learning',
  'nova-attention-control',
  'nova-ai-metrics',
  'nova-vision',
  'nova-code-proposal',
  'nova-github'
]);

const BLOCKED = new Set([
  'nova-guardian-intelligence',
  'guardian-repair-executor',
  'guardian-repair-worker',
  'guardian-worker',
  'guardian-repair-status',
  'admin-pricing-control',
  'admin-user-control'
]);

export function isSafeClientFunction(name) {
  const value = String(name || '');
  return SAFE_CLIENT_FUNCTIONS.includes(value) && !BLOCKED.has(value);
}
