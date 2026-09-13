import { createChatAdapter } from './adapters/chat-adapter.mjs';
import { createKnowledgeReadAdapter, createLearningReadAdapter, createAttentionReadAdapter, createMetricsReadAdapter, createGithubStatusAdapter } from './adapters/read-adapters.mjs';
import { createVisionAdapter } from './adapters/vision-adapter.mjs';
import { createCodeProposalAdapter } from './adapters/code-proposal-adapter.mjs';

export function createSafeServices({ edgeClient } = {}) {
  if (!edgeClient || typeof edgeClient.invoke !== 'function') throw new TypeError('EDGE_CLIENT_REQUIRED');
  return Object.freeze({
    chat: createChatAdapter({ edgeClient }),
    knowledge: createKnowledgeReadAdapter({ edgeClient }),
    learning: createLearningReadAdapter({ edgeClient }),
    attention: createAttentionReadAdapter({ edgeClient }),
    metrics: createMetricsReadAdapter({ edgeClient }),
    github: createGithubStatusAdapter({ edgeClient }),
    vision: createVisionAdapter({ edgeClient }),
    codeProposal: createCodeProposalAdapter({ edgeClient })
  });
}
