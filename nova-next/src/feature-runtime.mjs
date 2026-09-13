import { RUNTIME_CONFIG } from './runtime-config.mjs';
import { SAFE_CLIENT_FUNCTIONS } from './safe-client-functions.mjs';
import { createEdgeFunctionClient } from './adapters/edge-function-client.mjs';
import { createSafeServices } from './safe-services.mjs';

function freezeItems(items) {
  return Object.freeze(items.map(item => Object.freeze(item)));
}

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

  function automationStatus() {
    const safe = name => SAFE_CLIENT_FUNCTIONS.includes(name);
    return freezeItems([
      { id: 'guarded_chat', label: 'Guarded Chat', state: safe('nova-orchestrator') ? 'available' : 'staged', mode: 'interactive', detail: 'Admin-authenticated Nova orchestration is available through the guarded chat runtime.' },
      { id: 'vision', label: 'Nova Vision', state: safe('nova-vision') ? 'available' : 'staged', mode: 'interactive', detail: 'Supported images can be analysed only after an explicit user action.' },
      { id: 'knowledge', label: 'Knowledge', state: safe('nova-knowledge') ? 'available' : 'staged', mode: 'read-only', detail: 'Summary, list, search and get are exposed; mutation is not exposed to Nova Next.' },
      { id: 'github_broker', label: 'GitHub broker', state: safe('nova-github') ? 'available' : 'staged', mode: 'status-only', detail: 'Nova Next can read broker status only; merge, deploy, release and workflow mutation are not exposed.' },
      { id: 'code_proposal', label: 'Code proposals', state: safe('nova-code-proposal') ? 'available' : 'staged', mode: 'proposal-only', detail: 'Nova can prepare Nova Next code proposals, but cannot apply, merge or deploy them.' },
      { id: 'scheduling', label: 'Scheduling', state: 'staged', mode: 'staged', detail: 'No scheduler or background workflow executor is connected in this slice.' },
      { id: 'guardian_repair', label: 'Guardian repair', state: 'protected', mode: 'unavailable', detail: 'Repair execution and approval remain outside the Nova Next client.' },
      { id: 'release_ota', label: 'Release & OTA', state: 'protected', mode: 'unavailable', detail: 'Release, deployment, signing and OTA authority remain protected.' },
      { id: 'pricing_write', label: 'Pricing writes', state: 'protected', mode: 'unavailable', detail: 'Pricing approval and write authority are not exposed.' }
    ]);
  }

  async function integrationStatus() {
    let authenticated = false;
    try {
      authenticated = Boolean(String(getAccessToken() || '').trim());
    } catch {
      authenticated = false;
    }

    const items = [{
      id: 'nova_session',
      label: 'Nova / Supabase session',
      state: authenticated ? 'connected' : 'disconnected',
      readOnly: false,
      detail: authenticated
        ? 'Authenticated Admin session is active for approved Nova services.'
        : 'No authenticated Nova Admin session is active.'
    }];

    if (!authenticated) {
      items.push({
        id: 'github_broker',
        label: 'GitHub broker',
        state: 'disconnected',
        readOnly: true,
        detail: 'Broker status requires an authenticated Nova Admin session.'
      });
      return freezeItems(items);
    }

    try {
      const status = await services.github.status();
      const configured = status?.configured === true;
      items.push({
        id: 'github_broker',
        label: 'GitHub broker',
        state: configured ? 'connected' : 'disconnected',
        readOnly: true,
        detail: configured
          ? 'Read-only broker status is configured. Nova Next exposes no GitHub mutation action.'
          : 'The broker responded but is not configured.'
      });
    } catch (error) {
      items.push({
        id: 'github_broker',
        label: 'GitHub broker',
        state: 'unavailable',
        readOnly: true,
        detail: String(error?.message || error || 'GitHub broker status is unavailable.')
      });
    }

    return freezeItems(items);
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
    controlCentre,
    automationStatus,
    integrationStatus
  });
}
