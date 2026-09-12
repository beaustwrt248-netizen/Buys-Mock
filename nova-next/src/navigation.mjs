export const PRIMARY_NAV = Object.freeze(['Home', 'Chat', 'Tools', 'Tasks', 'More']);

export const DRAWER_NAV = Object.freeze([
  'Home',
  'Chat',
  'Tools',
  'Tasks',
  'Projects',
  'Knowledge Base',
  'Files',
  'Automation',
  'Calendar',
  'Integrations',
  'Settings',
  'Help & Support'
]);

const ROUTE_ALIASES = Object.freeze({
  'knowledge-base': 'knowledge',
  'help-&-support': 'help',
  'help-and-support': 'help'
});

const VALID_ROUTES = new Set([
  'home', 'chat', 'tools', 'tasks', 'more', 'projects', 'knowledge', 'files',
  'automation', 'calendar', 'integrations', 'settings', 'help'
]);

export function resolveRoute(route) {
  const normalized = String(route || '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/^#\/?/, '');
  const aliased = ROUTE_ALIASES[normalized] || normalized;
  return VALID_ROUTES.has(aliased) ? aliased : 'home';
}
