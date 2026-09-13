const MODES = new Set(['auto', 'single', 'ensemble']);
const PROVIDERS = new Set(['auto', 'gpt', 'gemini', 'claude', 'consensus']);
const DEGRADED_MODES = new Set(['ensemble-degraded', 'ensemble-unfused-budget', 'ensemble-unfused']);

export function createChatAdapter({ edgeClient } = {}) {
  if (!edgeClient || typeof edgeClient.invoke !== 'function') throw new TypeError('CHAT_EDGE_CLIENT_REQUIRED');

  async function ask(prompt, { mode = 'auto', provider = 'auto', highStakes } = {}) {
    const clean = String(prompt || '').trim();
    if (!clean) throw new Error('PROMPT_REQUIRED');
    if (!MODES.has(mode)) throw new Error('INVALID_MODE');
    if (!PROVIDERS.has(provider)) throw new Error('INVALID_PROVIDER');

    const body = { prompt: clean };
    if (provider !== 'auto') body.provider = provider;
    if (mode !== 'auto') body.mode = mode;
    if (highStakes === true) body.high_stakes = true;

    const raw = await edgeClient.invoke('nova-orchestrator', body);
    const resultMode = String(raw?.mode || 'unknown');
    return Object.freeze({
      ok: raw?.ok === true,
      answer: String(raw?.answer || ''),
      mode: resultMode,
      provider: String(raw?.provider || provider),
      guarded: raw?.guarded === true,
      degraded: DEGRADED_MODES.has(resultMode) || Array.isArray(raw?.failures) && raw.failures.length > 0,
      code: raw?.code ? String(raw.code) : null,
      modelsUsed: Array.isArray(raw?.models_used) ? raw.models_used.slice() : [],
      failures: Array.isArray(raw?.failures) ? raw.failures.slice() : []
    });
  }

  return Object.freeze({ ask });
}
