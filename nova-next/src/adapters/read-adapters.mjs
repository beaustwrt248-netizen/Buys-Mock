function requireEdge(edgeClient) {
  if (!edgeClient || typeof edgeClient.invoke !== 'function') throw new TypeError('EDGE_CLIENT_REQUIRED');
  return edgeClient;
}

const clean = (value, max = 180) => String(value ?? '').trim().slice(0, max);
const clampInt = (value, min, max, fallback) => {
  const n = Number(value);
  return Number.isInteger(n) ? Math.min(max, Math.max(min, n)) : fallback;
};

export function createKnowledgeReadAdapter({ edgeClient } = {}) {
  const edge = requireEdge(edgeClient);
  return Object.freeze({
    summary: () => edge.invoke('nova-knowledge', { action: 'summary' }),
    list: ({ category = 'all', limit = 50 } = {}) => edge.invoke('nova-knowledge', {
      action: 'list',
      category: clean(category, 40).toLowerCase() || 'all',
      limit: clampInt(limit, 1, 100, 50)
    }),
    search: (query, { category = 'all', limit = 20 } = {}) => {
      const q = clean(query, 180);
      if (!q) return Promise.reject(new Error('QUERY_REQUIRED'));
      return edge.invoke('nova-knowledge', {
        action: 'search',
        q,
        category: clean(category, 40).toLowerCase() || 'all',
        limit: clampInt(limit, 1, 20, 20)
      });
    },
    get: id => {
      const value = clean(id, 80);
      if (!value) return Promise.reject(new Error('KNOWLEDGE_ID_REQUIRED'));
      return edge.invoke('nova-knowledge', { action: 'get', id: value });
    }
  });
}

export function createLearningReadAdapter({ edgeClient } = {}) {
  const edge = requireEdge(edgeClient);
  return Object.freeze({ summary: () => edge.invoke('nova-learning', { action: 'summary' }) });
}

export function createAttentionReadAdapter({ edgeClient } = {}) {
  const edge = requireEdge(edgeClient);
  return Object.freeze({
    list: ({ limit = 120 } = {}) => edge.invoke('nova-attention-control', {
      action: 'list',
      limit: clampInt(limit, 20, 200, 120)
    })
  });
}

export function createMetricsReadAdapter({ edgeClient } = {}) {
  const edge = requireEdge(edgeClient);
  return Object.freeze({ summary: () => edge.invoke('nova-ai-metrics', {}) });
}

export function createGithubStatusAdapter({ edgeClient } = {}) {
  const edge = requireEdge(edgeClient);
  return Object.freeze({ status: () => edge.invoke('nova-github', { action: 'status' }) });
}
