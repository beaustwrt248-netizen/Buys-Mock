import { RUNTIME_CONFIG } from './runtime-config.mjs';
import { SAFE_CLIENT_FUNCTIONS } from './safe-client-functions.mjs';
import { createEdgeFunctionClient } from './adapters/edge-function-client.mjs';
import { createSafeServices } from './safe-services.mjs';

export function createFeatureRuntime({
  config = RUNTIME_CONFIG,
  getAccessToken,
  fetchImpl = globalThis.fetch,
  edgeClient = null
} = {}) {
  if (typeof getAccessToken !== 'function') throw new TypeError('ACCESS_TOKEN_PROVIDER_REQUIRED');
  const edge = edgeClient || createEdgeFunctionClient({
    baseUrl: config.supabaseUrl,
    publishableKey: config.supabasePublishableKey,
    getAccessToken,
    fetchImpl,
    allowedFunctions: SAFE_CLIENT_FUNCTIONS
  });
  const services = createSafeServices({ edgeClient: edge });

  async function controlCentre() {
    const [metrics, attention, github, learning] = await Promise.allSettled([
      services.metrics.summary(),
      services.attention.list(),
      services.github.status(),
      services.learning.summary()
    ]);
    const unwrap = result => result.status === 'fulfilled'
      ? { ok: true, data: result.value }
      : { ok: false, error: String(result.reason?.message || result.reason || 'unavailable') };
    return Object.freeze({
      metrics: unwrap(metrics),
      attention: unwrap(attention),
      github: unwrap(github),
      learning: unwrap(learning)
    });
  }

  return Object.freeze({
    knowledgeSummary: () => services.knowledge.summary(),
    knowledgeList: options => services.knowledge.list(options),
    knowledgeSearch: (query, options) => services.knowledge.search(query, options),
    knowledgeGet: id => services.knowledge.get(id),
    learningSummary: () => services.learning.summary(),
    attentionList: options => services.attention.list(options),
    metricsSummary: () => services.metrics.summary(),
    githubStatus: () => services.github.status(),
    analyseImages: (images, options) => services.vision.analyse(images, options),
    proposeCode: input => services.codeProposal.propose(input),
    controlCentre
  });
}
