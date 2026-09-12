const capability = (id, label, section, risk, adapter, evidenceRequired = true) => Object.freeze({
  id,
  label,
  section,
  status: 'parity-target',
  risk,
  requiresAuth: true,
  adapter,
  evidenceRequired
});

export const CAPABILITIES = Object.freeze([
  capability('conversation', 'AI Chat', 'chat', 'medium', 'conversation'),
  capability('command-discovery', 'Command Discovery', 'chat', 'low', 'commands'),
  capability('multi-step-jobs', 'Multi-step Jobs', 'tasks', 'medium', 'jobs'),
  capability('web-research', 'Live Web Research', 'tools', 'medium', 'research'),
  capability('camera-vision', 'Camera & Image Analysis', 'tools', 'medium', 'vision'),
  capability('catalogue-intelligence', 'Catalogue Intelligence', 'tools', 'medium', 'catalogue'),
  capability('pricing-intelligence', 'Pricing Intelligence', 'tools', 'high', 'pricing'),
  capability('support-intelligence', 'Support Intelligence', 'tools', 'medium', 'support'),
  capability('business-intelligence', 'Business Intelligence', 'tools', 'medium', 'business'),
  capability('scenario-planning', 'Scenario Planning', 'tools', 'low', 'scenario'),
  capability('voice', 'Voice Assistant', 'tools', 'medium', 'voice', false),
  capability('memory', 'Memory', 'knowledge', 'medium', 'memory'),
  capability('learning-controls', 'Learning Controls', 'knowledge', 'high', 'learning'),
  capability('evidence-review', 'Evidence Review', 'knowledge', 'medium', 'evidence'),
  capability('explain-mode', 'Explain Mode', 'knowledge', 'low', 'explain'),
  capability('guardian-analysis', 'Guardian Analysis', 'control-centre', 'high', 'guardian'),
  capability('release-readiness', 'Release Readiness', 'control-centre', 'high', 'release'),
  capability('bug-triage', 'Bug Triage', 'control-centre', 'medium', 'bugs'),
  capability('proactive-alerts', 'Proactive Alerts', 'control-centre', 'medium', 'alerts')
]);

export function capabilitiesForSection(section) {
  return CAPABILITIES.filter(item => item.section === section);
}

export function capabilityById(id) {
  return CAPABILITIES.find(item => item.id === id) || null;
}
