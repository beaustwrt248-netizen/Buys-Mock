function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

const APPEARANCE_CSS = `
:root[data-appearance="light"] { color-scheme: light; --bg:#f3f7fc; --bg-deep:#e8f0fa; --panel:rgba(255,255,255,.92); --panel-strong:#ffffff; --panel-soft:#edf4fb; --line:rgba(44,84,126,.2); --line-strong:rgba(37,117,198,.42); --text:#102033; --muted:#58718a; }
:root[data-appearance="light"] body { background:#e8f0fa; color:var(--text); }
:root[data-appearance="light"] .app-frame { background:radial-gradient(circle at 50% -10%,#dcecff 0,#f3f7fc 36%,#e8f0fa 100%); }
:root[data-appearance="light"] .page, :root[data-appearance="light"] .shell { color:var(--text); }
:root[data-appearance="light"] .access-card, :root[data-appearance="light"] .suggestion-list button, :root[data-appearance="light"] .feature-card, :root[data-appearance="light"] .feature-status-card, :root[data-appearance="light"] .workspace-task, :root[data-appearance="light"] .calendar-item, :root[data-appearance="light"] .file-card { background:rgba(255,255,255,.82); color:var(--text); }
:root[data-appearance="light"] .composer, :root[data-appearance="light"] .ask-bar { background:rgba(255,255,255,.86); }
:root[data-appearance="light"] .composer input { color:var(--text); }
@media (prefers-reduced-motion: reduce) { .page { animation:none!important; } *, *::before, *::after { scroll-behavior:auto!important; transition-duration:.01ms!important; animation-duration:.01ms!important; animation-iteration-count:1!important; } }
`;

export function createSettingsUi({
  documentObj = globalThis.document,
  preferences,
  getAccount = () => null,
  onNavigate = () => {},
  onToast = () => {}
} = {}) {
  if (!documentObj) throw new TypeError('SETTINGS_DOCUMENT_REQUIRED');
  if (!preferences) throw new TypeError('SETTINGS_PREFERENCES_REQUIRED');
  let returnFocus = null;

  function ensureAppearanceStyles() {
    if (documentObj.getElementById('novaNextAppearanceStyles')) return;
    const style = el(documentObj, 'style');
    style.id = 'novaNextAppearanceStyles';
    style.textContent = APPEARANCE_CSS;
    documentObj.head.append(style);
  }

  function closeSheet() {
    documentObj.querySelector('.settings-sheet-backdrop')?.remove();
    returnFocus?.focus?.({ preventScroll: true });
    returnFocus = null;
  }

  function showSheet(title, body) {
    closeSheet();
    returnFocus = documentObj.activeElement;
    const backdrop = el(documentObj, 'div', 'feature-sheet-backdrop settings-sheet-backdrop');
    const sheet = el(documentObj, 'section', 'feature-sheet');
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    sheet.setAttribute('aria-label', title);
    const head = el(documentObj, 'div', 'feature-sheet-head');
    head.append(el(documentObj, 'h2', '', title));
    const close = el(documentObj, 'button', 'icon-button', '×');
    close.type = 'button';
    close.setAttribute('aria-label', `Close ${title}`);
    close.addEventListener('click', closeSheet);
    head.append(close);
    sheet.append(head, body);
    backdrop.append(sheet);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) closeSheet(); });
    backdrop.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeSheet();
      }
    });
    documentObj.body.append(backdrop);
    close.focus({ preventScroll: true });
    return backdrop;
  }

  function renderAccount() {
    const account = getAccount() || {};
    const body = el(documentObj, 'div', 'feature-stack');
    body.append(
      el(documentObj, 'p', 'feature-boundary', 'Account details are read-only in Nova Next. Admin session security remains managed by the guarded authentication runtime.'),
      el(documentObj, 'article', 'feature-card', account.email ? `Admin session: ${account.email}` : 'Admin session identity is unavailable.')
    );
    showSheet('Account', body);
  }

  function applyAppearance(value) {
    ensureAppearanceStyles();
    const selected = value === 'system' ? '' : value;
    if (selected) documentObj.documentElement.setAttribute('data-appearance', selected);
    else documentObj.documentElement.removeAttribute('data-appearance');
  }

  function renderAppearance() {
    const body = el(documentObj, 'div', 'feature-stack');
    body.append(el(documentObj, 'p', 'feature-boundary', 'Appearance is a Nova Next local preference and does not change other Morley apps.'));
    for (const value of ['system', 'dark', 'light']) {
      const button = el(documentObj, 'button', 'secondary-button', value[0].toUpperCase() + value.slice(1));
      button.type = 'button';
      button.setAttribute('aria-pressed', String(preferences.get().appearance === value));
      button.addEventListener('click', () => {
        try {
          preferences.setAppearance(value);
          applyAppearance(value);
          renderAppearance();
          onToast(`Appearance set to ${value}.`);
        } catch {
          onToast('Appearance preference could not be saved.', 'error');
        }
      });
      body.append(button);
    }
    showSheet('Appearance', body);
  }

  function renderNotifications() {
    const body = el(documentObj, 'div', 'feature-stack');
    body.append(el(documentObj, 'p', 'feature-boundary', 'This is a local preference only. Nova Next does not register a background notification service in this slice.'));
    const toggle = el(documentObj, 'button', 'secondary-button', preferences.get().notifications ? 'Disable local notification preference' : 'Enable local notification preference');
    toggle.type = 'button';
    toggle.setAttribute('aria-pressed', String(preferences.get().notifications));
    toggle.addEventListener('click', () => {
      try {
        preferences.setNotifications(!preferences.get().notifications);
        renderNotifications();
      } catch {
        onToast('Notification preference could not be saved.', 'error');
      }
    });
    body.append(toggle);
    showSheet('Notifications', body);
  }

  function handleAction(action) {
    if (action === 'settings-account') return renderAccount();
    if (action === 'settings-appearance') return renderAppearance();
    if (action === 'settings-notifications') return renderNotifications();
    if (action === 'settings-privacy') {
      onNavigate('files');
      onToast('Privacy and data boundaries are shown in the Files session workspace.');
      return true;
    }
    if (action === 'settings-about') {
      onNavigate('help');
      return true;
    }
    return false;
  }

  function bind() {
    applyAppearance(preferences.get().appearance);
  }

  return Object.freeze({ bind, handleAction, renderAccount, renderAppearance, renderNotifications, closeSheet });
}
