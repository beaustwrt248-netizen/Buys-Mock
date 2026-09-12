const POLICY = Object.freeze({
  'catalogue-audit-read': { risk: 'low', protected: false, auto: true },
  'evidence-read': { risk: 'low', protected: false, auto: true },
  'support-read': { risk: 'low', protected: false, auto: true },
  'guardian-status-read': { risk: 'low', protected: false, auto: true },
  'release-read': { risk: 'low', protected: false, auto: true },
  'business-metrics-read': { risk: 'low', protected: false, auto: true },
  'web-research': { risk: 'medium', protected: false, auto: true },
  'draft-support-response': { risk: 'medium', protected: false, auto: true },
  'prepare-catalogue-finding': { risk: 'medium', protected: false, auto: true },
  'pricing-write': { risk: 'high', protected: true, auto: false },
  'guardian-repair-approve': { risk: 'high', protected: true, auto: false },
  'guardian-repair-execute': { risk: 'high', protected: true, auto: false },
  'deploy': { risk: 'high', protected: true, auto: false },
  'ota-publish': { risk: 'high', protected: true, auto: false },
  'signing-change': { risk: 'high', protected: true, auto: false },
  'role-change': { risk: 'high', protected: true, auto: false },
  'user-change': { risk: 'high', protected: true, auto: false },
  'destructive-delete': { risk: 'high', protected: true, auto: false },
  'catalogue-patch-apply': { risk: 'high', protected: true, auto: false }
});

const UNKNOWN = Object.freeze({ risk: 'high', protected: true, auto: false, reason: 'unknown-action-fails-closed' });

export function classifyAction(action) {
  const policy = POLICY[String(action || '')];
  return policy ? Object.freeze({ ...policy, action }) : Object.freeze({ ...UNKNOWN, action: String(action || '') });
}

export function mayAutoExecute(action) {
  return classifyAction(action).auto === true;
}

export function knownActions() {
  return Object.keys(POLICY);
}
