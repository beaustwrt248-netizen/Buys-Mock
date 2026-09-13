function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

export function createSettingsUi({
  documentObj = globalThis.document,
  preferences,
  getAccount = () => null,
  onNavigate = () => {},
  onToast = () => {}
} = {}) {
  if (!documentObj) throw new TypeError('SETTINGS_DOCUMENT_REQUIRED');
  if (!preferences) throw new TypeError('SETTINGS_PREFERENCES_REQUIRED');

  function closeSheet() {
    documentObj.querySelector('.settings-sheet-backdrop')?.remove();
  }

  function showSheet(title, body) {
    closeSheet();
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

  return Object.freeze({ bind, handleAction, renderAccount, renderAppearance, renderNotifications });
}
