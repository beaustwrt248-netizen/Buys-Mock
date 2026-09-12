import { PRIMARY_NAV, DRAWER_NAV, resolveRoute } from './src/navigation.mjs';

const splashView = document.getElementById('splashView');
const loginView = document.getElementById('loginView');
const shell = document.getElementById('shell');
const allSetView = document.getElementById('allSetView');
const drawer = document.getElementById('drawer');
const drawerBackdrop = document.getElementById('drawerBackdrop');
const menuButton = document.getElementById('menuButton');
const drawerNav = document.getElementById('drawerNav');
const bottomNav = document.getElementById('bottomNav');
const pages = [...document.querySelectorAll('.page[data-route]')];
const subtitle = document.getElementById('topbarSubtitle');

const ROUTE_LABELS = {
  home: 'Your AI-Powered Assistant', chat: 'Your AI Assistant', tools: 'AI Utilities', tasks: 'Stay organised',
  more: 'More from Nova', projects: 'Manage and build', knowledge: 'Saved knowledge', files: 'Your files',
  automation: 'Schedules and workflows', calendar: 'Plan your work', integrations: 'Connected tools',
  settings: 'Customise Nova', help: 'Help & Support'
};

const DRAWER_ICONS = ['⌂','◉','⌘','☑','▣','◇','▤','◴','▦','⌁','⚙','?'];
const BOTTOM_ICONS = { Home: '⌂', Chat: '◉', Tools: '⌘', Tasks: '☑', More: '•••' };

let currentRoute = 'home';

function labelToRoute(label) {
  return resolveRoute(label);
}

function buildNavigation() {
  drawerNav.replaceChildren(...DRAWER_NAV.map((label, index) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = label;
    button.dataset.routeTarget = labelToRoute(label);
    button.dataset.icon = DRAWER_ICONS[index] || '•';
    return button;
  }));

  bottomNav.replaceChildren(...PRIMARY_NAV.map(label => {
    const button = document.createElement('button');
    button.type = 'button';
    button.dataset.routeTarget = labelToRoute(label);
    const icon = document.createElement('span');
    icon.className = 'nav-icon';
    icon.textContent = BOTTOM_ICONS[label];
    const text = document.createElement('span');
    text.textContent = label;
    button.append(icon, text);
    return button;
  }));
}

function setRoute(route, { closeDrawer = true } = {}) {
  currentRoute = resolveRoute(route);
  for (const page of pages) page.classList.toggle('is-active', page.dataset.route === currentRoute);
  for (const button of document.querySelectorAll('[data-route-target]')) {
    if (button.closest('.bottom-nav, .drawer-nav')) {
      const selected = resolveRoute(button.dataset.routeTarget) === currentRoute;
      button.toggleAttribute('aria-current', selected);
      if (selected) button.setAttribute('aria-current', 'page');
    }
  }
  subtitle.textContent = ROUTE_LABELS[currentRoute] || 'Your AI-Powered Assistant';
  if (closeDrawer) setDrawer(false);
}

function setDrawer(open) {
  drawer.classList.toggle('is-open', open);
  drawer.setAttribute('aria-hidden', String(!open));
  menuButton.setAttribute('aria-expanded', String(open));
  drawerBackdrop.hidden = !open;
  if (open) drawer.querySelector('[aria-current="page"]')?.focus({ preventScroll: true });
}

function showOnly(view) {
  for (const item of [splashView, loginView, shell, allSetView]) item.classList.toggle('is-hidden', item !== view);
}

function bootstrap() {
  buildNavigation();
  setRoute('home', { closeDrawer: false });
  window.setTimeout(() => showOnly(loginView), 850);
}

menuButton.addEventListener('click', () => setDrawer(!drawer.classList.contains('is-open')));
drawerBackdrop.addEventListener('click', () => setDrawer(false));

document.addEventListener('click', event => {
  const routeButton = event.target.closest('[data-route-target]');
  if (routeButton) {
    setRoute(routeButton.dataset.routeTarget);
    return;
  }

  const action = event.target.closest('[data-action]')?.dataset.action;
  if (action === 'toggle-password') {
    const input = document.querySelector('input[name="password"]');
    input.type = input.type === 'password' ? 'text' : 'password';
  } else if (action === 'signout') {
    setDrawer(false);
    showOnly(loginView);
  } else if (action === 'finish-intro') {
    showOnly(shell);
    setRoute('home');
  }
});

document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && drawer.classList.contains('is-open')) setDrawer(false);
});

document.getElementById('loginForm').addEventListener('submit', event => {
  event.preventDefault();
  showOnly(allSetView);
});

for (const tabList of document.querySelectorAll('.filter-tabs')) {
  tabList.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) return;
    for (const candidate of tabList.querySelectorAll('button')) candidate.classList.toggle('is-selected', candidate === button);
  });
}

bootstrap();
