export const RUNTIME_CONFIG = Object.freeze({
  supabaseUrl: 'https://ghdhairijqjqivqriigi.supabase.co',
  supabasePublishableKey: 'sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt',
  turnstileSiteKey: '0x4AAAAAAEZul-Qo6dqMim2U',
  sessionKey: 'nova-next.session.v1',
  chatFunction: 'nova-orchestrator'
});

export function assertPublicRuntimeConfig(config = RUNTIME_CONFIG) {
  for (const [name, value] of Object.entries(config)) {
    if (!String(value || '').trim()) throw new Error(`RUNTIME_CONFIG_${name.toUpperCase()}_MISSING`);
  }
  const joined = JSON.stringify(config);
  if (/service[_-]?role|openrouter|sk-[A-Za-z0-9_-]{20,}/i.test(joined)) {
    throw new Error('RUNTIME_CONFIG_CONTAINS_PRIVILEGED_SECRET');
  }
  return true;
}
