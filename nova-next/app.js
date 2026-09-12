import { PRIMARY_NAV, DRAWER_NAV, resolveRoute } from './src/navigation.mjs';
import { createRouter } from './src/router.mjs';
import { createLiveRuntime } from './src/live-runtime.mjs';

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
const router = createRouter({ initialRoute: 'home' });
let liveRuntime = null;
let toastTimer = null;

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
  router.go(route);
  const currentRoute = router.current();
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

function showToast(message, tone = '') {
  document.querySelector('.runtime-toast')?.remove();
  if (toastTimer) clearTimeout(toastTimer);
  const toast = document.createElement('div');
  toast.className = `runtime-toast${tone ? ` ${tone}` : ''}`;
  toast.setAttribute('role', 'status');
  toast.textContent = message;
  document.body.append(toast);
  toastTimer = setTimeout(() => toast.remove(), 3600);
}

function ensureHeadLink(rel, href, id) {
  if (document.getElementById(id)) return;
  const link = document.createElement('link');
  link.id = id;
  link.rel = rel;
  link.href = href;
  document.head.append(link);
}

function ensureIsolatedAssets() {
  ensureHeadLink('manifest', './manifest.webmanifest', 'novaNextManifest');
  ensureHeadLink('stylesheet', './live.css', 'novaNextLiveStyles');
}

function registerIsolatedServiceWorker() {
  if (!('serviceWorker' in navigator)) return;
  const base = new URL('./', window.location.href);
  if (!base.pathname.endsWith('/nova-next/')) return;
  navigator.serviceWorker.register('./service-worker.js', { scope: './' }).catch(error => {
    console.warn('nova-next service worker registration failed', error);
  });
}

function markAuthenticatedSession(session) {
  const email = String(session?.user?.email || '');
  const online = document.querySelector('.assistant-row p');
  if (online) online.innerHTML = '<span class="online-dot"></span> Guarded · Admin session';
  const preview = document.getElementById('previewNote');
  if (preview && email) preview.textContent = `Signed in securely as ${email}.`;
}

async function bootstrap() {
  ensureIsolatedAssets();
  buildNavigation();
  setRoute('home', { closeDrawer: false });
  registerIsolatedServiceWorker();

  liveRuntime = createLiveRuntime({
    callbacks: {
      onAuthenticated(session, { restored } = {}) {
        markAuthenticatedSession(session);
        showOnly(restored ? shell : allSetView);
      },
      onLocked(reason) {
        showOnly(loginView);
        if (reason === 'AUTH_REQUIRED') showToast('Your Nova session expired. Sign in again.', 'error');
      },
      onChatResult(result) {
        if (result.degraded) showToast('Nova answered in degraded mode. Review the response evidence carefully.');
      },
      onRuntimeError(error) {
        console.error('nova-next runtime', error);
      }
    }
  });

  try {
    await liveRuntime.start();
  } catch (error) {
    console.error('nova-next boot', error);
    showOnly(loginView);
    showToast('Secure Nova startup failed. Access remains locked.', 'error');
  }
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
    liveRuntime?.signOut();
  } else if (action === 'finish-intro') {
    showOnly(shell);
    setRoute('home');
  }

  const tool = event.target.closest('[data-tool]');
  if (tool) {
    showToast('This Nova tool is staged in the new interface but its live adapter is not connected in this security phase yet.');
  }
});

document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && drawer.classList.contains('is-open')) setDrawer(false);
});

for (const tabList of document.querySelectorAll('.filter-tabs')) {
  tabList.addEventListener('click', event => {
    const button = event.target.closest('button');
    if (!button) return;
    for (const candidate of tabList.querySelectorAll('button')) candidate.classList.toggle('is-selected', candidate === button);
  });
}

bootstrap();
