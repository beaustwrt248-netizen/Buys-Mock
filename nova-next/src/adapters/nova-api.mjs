import { classifyAction, mayAutoExecute } from '../action-policy.mjs';

function normalizeResult(result) {
  if (!result || typeof result !== 'object') {
    return { ok: false, data: null, evidence: [], error: { code: 'INVALID_RESPONSE', message: 'Adapter returned an invalid response.' } };
  }
  return {
    ok: result.ok === true,
    data: result.data ?? null,
    evidence: Array.isArray(result.evidence) ? result.evidence : [],
    error: result.error ?? null
  };
}

export function createNovaApi({ getAccessToken, transport }) {
  if (typeof getAccessToken !== 'function') throw new TypeError('getAccessToken must be a function');
  if (typeof transport !== 'function') throw new TypeError('transport must be a function');

  async function run(action, payload = {}) {
    const policy = classifyAction(action);

    if (policy.protected) {
      const knownProtected = policy.reason !== 'unknown-action-fails-closed';
      return {
        ok: false,
        data: null,
        evidence: [],
        error: {
          code: knownProtected ? 'APPROVAL_REQUIRED' : 'ACTION_BLOCKED',
          message: knownProtected
            ? 'This protected action requires the existing human approval path.'
            : 'Unknown actions fail closed.'
        },
        policy
      };
    }

    if (!mayAutoExecute(action)) {
      return {
        ok: false,
        data: null,
        evidence: [],
        error: { code: 'ACTION_BLOCKED', message: 'Action is not allowed for automatic execution.' },
        policy
      };
    }

    const token = await getAccessToken();
    if (!token) {
      return {
        ok: false,
        data: null,
        evidence: [],
        error: { code: 'AUTH_REQUIRED', message: 'An authenticated Admin session is required.' },
        policy
      };
    }

    try {
      const response = await transport({
        action,
        payload,
        headers: { Authorization: `Bearer ${token}` }
      });
      return { ...normalizeResult(response), policy };
    } catch (error) {
      return {
        ok: false,
        data: null,
        evidence: [],
        error: { code: 'TRANSPORT_ERROR', message: error instanceof Error ? error.message : 'Request failed.' },
        policy
      };
    }
  }

  return Object.freeze({ run });
}
