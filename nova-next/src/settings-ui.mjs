function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

export function createSettingsUi({ preferences, documentObj = globalThis.document, onToast = () => {} } = {}) {
  if (!preferences) throw new TypeError('PREFERENCES_REQUIRED');
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');
  let bound = false;

  function applyAppearance(appearance) {
    documentObj.documentElement.dataset.novaAppearance = appearance;
    documentObj.documentElement.style.colorScheme = appearance === 'system' ? 'light dark' : appearance;
  }

  function closeExisting() {
    documentObj.querySelector('.feature-sheet-backdrop')?.remove();
  }

  function showSheet(title, body) {
    closeExisting();
    const backdrop = el(documentObj, 'div', 'feature-sheet-backdrop');
    const sheet = el(documentObj, 'section', 'feature-sheet');
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    const head = el(documentObj, 'div', 'feature-sheet-head');
    head.append(el(documentObj, 'h2', '', title));
    const close = el(documentObj, 'button', 'icon-button', '×');
    close.type = 'button';
    close.setAttribute('aria-label', 'Close settings');
    close.addEventListener('click', () => backdrop.remove());
    head.append(close);
    sheet.append(head, body);
    backdrop.append(sheet);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) backdrop.remove(); });
    documentObj.body.append(backdrop);
  }

  function openAccount() {
    const body = el(documentObj, 'div', 'feature-stack');
    const profile = documentObj.querySelector('.drawer-profile .profile-copy');
    const name = profile?.querySelector('strong')?.textContent?.trim() || 'Morley Admin';
    const email = profile?.querySelector('small')?.textContent?.trim() || 'Authenticated account';
    const card = el(documentObj, 'article', 'feature-card');
    card.append(el(documentObj, 'small', '', 'Current guarded session'), el(documentObj, 'strong', '', name), el(documentObj, 'p', '', email));
    body.append(card, el(documentObj, 'p', 'feature-boundary', 'Account changes, passwords, roles and permissions are not modified from Nova Next Settings.'));
    showSheet('Account', body);
  }

  function openAppearance() {
    const body = el(documentObj, 'div', 'feature-stack');
    body.append(el(documentObj, 'p', 'feature-boundary', 'Appearance is stored only in the isolated Nova Next local preference namespace.'));
    const actions = el(documentObj, 'div', 'workspace-actions');
    for (const option of ['system', 'light', 'dark']) {
      const button = el(documentObj, 'button', option === preferences.get().appearance ? 'primary-button' : 'secondary-button', option[0].toUpperCase() + option.slice(1));
      button.type = 'button';
      button.addEventListener('click', () => {
        preferences.setAppearance(option);
        applyAppearance(option);
        onToast(`Appearance set to ${option}.`);
        openAppearance();
      });
      actions.append(button);
    }
    body.append(actions);
    showSheet('Appearance', body);
  }

  function openNotifications() {
    const current = preferences.get();
    const body = el(documentObj, 'div', 'feature-stack');
    const card = el(documentObj, 'article', 'feature-card');
    card.append(el(documentObj, 'strong', '', 'Nova Next notification preference'));
    card.append(el(documentObj, 'p', '', current.notifications ? 'Enabled locally' : 'Disabled locally'));
    const toggle = el(documentObj, 'button', 'primary-button', current.notifications ? 'Disable local preference' : 'Enable local preference');
    toggle.type = 'button';
    toggle.addEventListener('click', () => {
      preferences.setNotifications(!current.notifications);
      onToast('Notification preference updated locally. No background or system push permission was changed.');
      openNotifications();
    });
    body.append(card, toggle, el(documentObj, 'p', 'feature-boundary', 'This is a local preference only. It does not enable system push notifications, background alerts or scheduled monitoring.'));
    showSheet('Notifications', body);
  }

  function checkForUpdates() {
    const userAgent = String(globalThis.navigator?.userAgent || '');
    if (!userAgent.includes('NovaNextAndroid/')) {
      onToast('OTA app updates are available in Nova Next for Android.');
      return;
    }
    globalThis.location.href = 'novanext://check-updates';
  }

  function bindAction(action, handler) {
    const button = documentObj.querySelector(`[data-action="${action}"]`);
    if (!button) return;
    button.addEventListener('click', event => {
      event.preventDefault();
      event.stopImmediatePropagation();
      handler();
    });
  }

  function bind() {
    if (bound) return;
    bound = true;
    applyAppearance(preferences.get().appearance);
    bindAction('settings-account', openAccount);
    bindAction('settings-appearance', openAppearance);
    bindAction('settings-notifications', openNotifications);
    bindAction('check-updates', checkForUpdates);
  }

  return Object.freeze({ bind, openAccount, openAppearance, openNotifications, checkForUpdates, applyAppearance });
}
