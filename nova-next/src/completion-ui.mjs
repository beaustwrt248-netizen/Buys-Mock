export function filterToolRecords(records, { category = 'all', query = '' } = {}) {
  const normalizedCategory = String(category || 'all').trim().toLowerCase();
  const normalizedQuery = String(query || '').trim().toLowerCase();
  return records.filter(record => {
    const recordCategory = String(record?.category || '').trim().toLowerCase();
    const recordText = String(record?.text || '').trim().toLowerCase();
    const categoryMatches = normalizedCategory === 'all' || recordCategory === normalizedCategory;
    const queryMatches = !normalizedQuery || recordText.includes(normalizedQuery);
    return categoryMatches && queryMatches;
  });
}

function createStatusToast(documentObj, message, tone = '') {
  documentObj.querySelector('.completion-toast')?.remove();
  const toast = documentObj.createElement('div');
  toast.className = `completion-toast${tone ? ` ${tone}` : ''}`;
  toast.setAttribute('role', 'status');
  toast.textContent = message;
  documentObj.body.append(toast);
  globalThis.setTimeout?.(() => toast.remove(), 3600);
}

function bindToolFiltering(documentObj) {
  const page = documentObj.querySelector('.page[data-route="tools"]');
  const search = documentObj.getElementById('novaNextToolSearch');
  const filters = documentObj.getElementById('novaNextToolFilters');
  const list = documentObj.getElementById('novaNextToolList');
  const empty = documentObj.getElementById('novaNextToolEmpty');
  if (!page || !search || !filters || !list || page.dataset.filterBound === 'true') return;
  page.dataset.filterBound = 'true';

  const buttons = [...list.querySelectorAll('button[data-tool-category]')];
  let category = filters.querySelector('.is-selected')?.dataset.toolFilter || 'all';

  function filterTools() {
    const records = buttons.map(button => ({
      button,
      category: button.dataset.toolCategory,
      text: button.textContent
    }));
    const visible = new Set(filterToolRecords(records, { category, query: search.value }).map(record => record.button));
    for (const button of buttons) button.hidden = !visible.has(button);
    if (empty) empty.classList.toggle('is-hidden', visible.size !== 0);
  }

  search.addEventListener('input', filterTools);
  filters.addEventListener('click', event => {
    const button = event.target.closest('button[data-tool-filter]');
    if (!button) return;
    category = button.dataset.toolFilter || 'all';
    for (const candidate of filters.querySelectorAll('button[data-tool-filter]')) {
      const selected = candidate === button;
      candidate.classList.toggle('is-selected', selected);
      candidate.setAttribute('aria-pressed', String(selected));
    }
    filterTools();
  });

  filterTools();
}

function bindChatState(documentObj) {
  const page = documentObj.querySelector('.page[data-route="chat"]');
  const scroll = documentObj.getElementById('novaNextChatScroll');
  const empty = page?.querySelector('.chat-empty-state');
  if (!page || !scroll || !empty || page.dataset.completionChatBound === 'true') return;
  page.dataset.completionChatBound = 'true';
  let normalizing = false;

  function normalizeChat() {
    if (normalizing) return;
    normalizing = true;
    try {
      const containers = [...documentObj.querySelectorAll('#novaNextChatMessages')];
      let messages = containers.find(node => node.childElementCount > 0) || containers[0] || null;
      if (!messages) {
        messages = documentObj.createElement('div');
        messages.id = 'novaNextChatMessages';
        messages.className = 'chat-messages';
        messages.setAttribute('aria-live', 'polite');
      }
      for (const duplicate of containers) {
        if (duplicate === messages) continue;
        while (duplicate.firstChild) messages.append(duplicate.firstChild);
        duplicate.remove();
      }
      if (messages.parentElement !== scroll || messages.nextElementSibling !== empty) {
        scroll.insertBefore(messages, empty);
      }
      const active = messages.childElementCount === 0;
      empty.classList.toggle('is-active', active);
      empty.setAttribute('aria-hidden', String(!active));
      if (!active) scroll.scrollTop = scroll.scrollHeight;
    } finally {
      normalizing = false;
    }
  }

  const observer = new MutationObserver(() => queueMicrotask(normalizeChat));
  observer.observe(scroll, { childList: true, subtree: true });
  normalizeChat();
}

function showWebUpdateBanner(documentObj, registration) {
  if (documentObj.getElementById('novaNextWebUpdateBanner')) return;
  const banner = documentObj.createElement('div');
  banner.id = 'novaNextWebUpdateBanner';
  banner.className = 'web-update-banner';
  banner.setAttribute('role', 'status');
  const copy = documentObj.createElement('span');
  copy.textContent = 'A Nova web update is ready.';
  const reload = documentObj.createElement('button');
  reload.type = 'button';
  reload.textContent = 'Reload';
  reload.addEventListener('click', () => {
    registration?.waiting?.postMessage?.({ type: 'SKIP_WAITING' });
    globalThis.location?.reload?.();
  });
  const dismiss = documentObj.createElement('button');
  dismiss.type = 'button';
  dismiss.className = 'icon-button';
  dismiss.setAttribute('aria-label', 'Dismiss update notice');
  dismiss.textContent = '×';
  dismiss.addEventListener('click', () => banner.remove());
  banner.append(copy, reload, dismiss);
  documentObj.body.append(banner);
}

function isNativeNova(navigatorObj) {
  return /(?:^|\s)NovaNextAndroid\//.test(String(navigatorObj?.userAgent || ''));
}

function bindUpdateChecks(documentObj, navigatorObj) {
  const serviceWorker = navigatorObj?.serviceWorker;
  serviceWorker?.addEventListener('message', event => {
    if (event.data?.type !== 'NOVA_WEB_UPDATE_READY') return;
    serviceWorker.getRegistration('./').then(registration => showWebUpdateBanner(documentObj, registration));
  });

  documentObj.addEventListener('click', async event => {
    const button = event.target.closest('[data-action="check-updates"]');
    if (!button) return;
    button.disabled = true;
    createStatusToast(documentObj, 'Checking Nova updates…');
    try {
      if (isNativeNova(navigatorObj)) {
        globalThis.location.href = 'novanext://check-updates';
        createStatusToast(documentObj, 'Native Nova update check started.');
        return;
      }
      if (!serviceWorker) throw new Error('SERVICE_WORKER_UNAVAILABLE');
      const registration = await serviceWorker.getRegistration('./');
      if (registration) await registration.update();
      createStatusToast(documentObj, 'Nova web assets are up to date.');
    } catch (error) {
      console.error('nova-next update check', error);
      createStatusToast(documentObj, 'Nova could not check for updates. Try again when you are online.', 'error');
    } finally {
      button.disabled = false;
    }
  });
}

function enhanceControlCentre(documentObj) {
  const page = documentObj.querySelector('.page[data-route="more"]');
  if (!page || page.dataset.completionControlBound === 'true') return;
  page.dataset.completionControlBound = 'true';
  let enhancing = false;

  function apply() {
    if (enhancing) return;
    enhancing = true;
    try {
      const grid = page.querySelector('.feature-status-grid');
      if (grid && !page.querySelector('#novaNextOtaStatus')) {
        const card = documentObj.createElement('article');
        card.id = 'novaNextOtaStatus';
        card.className = 'feature-status-card ok';
        const label = documentObj.createElement('small');
        label.textContent = 'OTA updates';
        const primary = documentObj.createElement('strong');
        primary.textContent = 'Nova stable channel';
        const detail = documentObj.createElement('p');
        detail.textContent = 'Package identity, signing identity and SHA-256 integrity are verified before Android install handoff.';
        card.append(label, primary, detail);
        grid.append(card);
      }

      const boundary = page.querySelector('.feature-boundary');
      if (boundary) boundary.textContent = 'Operational changes remain protected and require the authorised Morley Admin workflow.';

      const hasError = Boolean(page.querySelector('.feature-status-card.error'));
      let retry = page.querySelector('#novaNextControlRetry');
      if (hasError && !retry) {
        retry = documentObj.createElement('button');
        retry.id = 'novaNextControlRetry';
        retry.type = 'button';
        retry.className = 'secondary-button control-retry';
        retry.textContent = 'Retry operational status';
        retry.addEventListener('click', () => globalThis.location?.reload?.());
        page.append(retry);
      } else if (!hasError) {
        retry?.remove();
      }
    } finally {
      enhancing = false;
    }
  }

  const observer = new MutationObserver(() => queueMicrotask(apply));
  observer.observe(page, { childList: true, subtree: true });
  apply();
}

export function bindCompletionUi({
  documentObj = globalThis.document,
  navigatorObj = globalThis.navigator
} = {}) {
  if (!documentObj) return;
  bindToolFiltering(documentObj);
  bindChatState(documentObj);
  bindUpdateChecks(documentObj, navigatorObj);
  enhanceControlCentre(documentObj);
}

if (globalThis.document) bindCompletionUi();
