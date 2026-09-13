function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function clear(node) {
  while (node?.firstChild) node.firstChild.remove();
}

function value(value, fallback = '—') {
  return value === null || value === undefined || value === '' ? fallback : String(value);
}

function fileToDataUrl(file, windowObj) {
  return new Promise((resolve, reject) => {
    const reader = new windowObj.FileReader();
    reader.onload = () => resolve(String(reader.result || ''));
    reader.onerror = () => reject(reader.error || new Error('FILE_READ_FAILED'));
    reader.readAsDataURL(file);
  });
}

export function createFeatureUi({ featureRuntime, documentObj = globalThis.document, windowObj = globalThis.window, onNavigate = () => {}, onToast = () => {}, onError = () => {} } = {}) {
  if (!featureRuntime) throw new TypeError('FEATURE_RUNTIME_REQUIRED');
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');
  let bound = false;
  let knowledgeLoaded = false;
  let controlLoaded = false;

  function showSheet(title, contentNode) {
    documentObj.querySelector('.feature-sheet-backdrop')?.remove();
    const backdrop = el(documentObj, 'div', 'feature-sheet-backdrop');
    const sheet = el(documentObj, 'section', 'feature-sheet');
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    const head = el(documentObj, 'div', 'feature-sheet-head');
    head.append(el(documentObj, 'h2', '', title));
    const close = el(documentObj, 'button', 'icon-button', '×');
    close.type = 'button';
    close.setAttribute('aria-label', 'Close');
    close.addEventListener('click', () => backdrop.remove());
    head.append(close);
    sheet.append(head, contentNode);
    backdrop.append(sheet);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) backdrop.remove(); });
    documentObj.body.append(backdrop);
    return { backdrop, sheet };
  }

  function renderVision(result) {
    const wrap = el(documentObj, 'div', 'feature-result');
    const data = result?.result || {};
    const hero = el(documentObj, 'div', 'feature-result-hero');
    hero.append(el(documentObj, 'strong', '', value(data.likely_model || data.likely_family || data.likely_brand, 'Device analysis')));
    hero.append(el(documentObj, 'small', '', `Confidence ${Math.round(Number(data.confidence || 0) * 100)}% · photos ${value(result?.photo_count, 0)}`));
    wrap.append(hero);
    const grid = el(documentObj, 'div', 'feature-kv-grid');
    for (const [label, field] of [['Brand',data.likely_brand],['Model number',data.model_number],['Storage',data.storage],['Colour',data.colour],['Grade',data.condition_grade]]) {
      const card = el(documentObj, 'div', 'feature-kv');
      card.append(el(documentObj, 'small', '', label), el(documentObj, 'strong', '', value(field)));
      grid.append(card);
    }
    wrap.append(grid);
    const evidence = Array.isArray(data.evidence) ? data.evidence : [];
    if (evidence.length) {
      wrap.append(el(documentObj, 'h3', '', 'Visible evidence'));
      const list = el(documentObj, 'ul', 'feature-list');
      for (const item of evidence.slice(0, 12)) list.append(el(documentObj, 'li', '', String(item)));
      wrap.append(list);
    }
    const uncertainty = Array.isArray(data.uncertainties) ? data.uncertainties : [];
    if (uncertainty.length) {
      wrap.append(el(documentObj, 'h3', '', 'Uncertainties'));
      const list = el(documentObj, 'ul', 'feature-list warning');
      for (const item of uncertainty.slice(0, 10)) list.append(el(documentObj, 'li', '', String(item)));
      wrap.append(list);
    }
    wrap.append(el(documentObj, 'p', 'feature-boundary', 'Advisory visual analysis only. Hidden functionality and identifiers are not inferred; images are not stored by Nova Vision.'));
    showSheet('Image Analysis', wrap);
  }

  async function pickImages() {
    const input = documentObj.createElement('input');
    input.type = 'file';
    input.accept = 'image/jpeg,image/png,image/webp';
    input.multiple = true;
    input.setAttribute('capture', 'environment');
    input.addEventListener('change', async () => {
      const files = [...(input.files || [])].slice(0, 6);
      if (!files.length) return;
      onToast('Analysing images with Nova Vision…');
      try {
        const urls = await Promise.all(files.map(file => fileToDataUrl(file, windowObj)));
        const result = await featureRuntime.analyseImages(urls, { hint: 'Identify the device and visible condition conservatively.' });
        renderVision(result);
      } catch (error) {
        onToast('Image analysis could not complete. No result was fabricated.', 'error');
        onError(error);
      }
    }, { once: true });
    input.click();
  }

  function openCodeProposal() {
    const wrap = el(documentObj, 'form', 'feature-form');
    const intro = el(documentObj, 'p', 'feature-boundary', 'Proposal only. Nova cannot apply, merge, deploy or modify protected paths from this tool.');
    const diagnostic = documentObj.createElement('textarea');
    diagnostic.placeholder = 'Describe the Nova Next UI/code problem…';
    diagnostic.rows = 6;
    diagnostic.required = true;
    const files = documentObj.createElement('input');
    files.type = 'text';
    files.value = 'nova-next/app.js, nova-next/styles.css';
    files.setAttribute('aria-label', 'Candidate files');
    const submit = el(documentObj, 'button', 'primary-button', 'Generate proposal');
    submit.type = 'submit';
    const result = el(documentObj, 'div', 'feature-result');
    wrap.append(intro, diagnostic, files, submit, result);
    wrap.addEventListener('submit', async event => {
      event.preventDefault();
      submit.disabled = true;
      submit.textContent = 'Analysing…';
      clear(result);
      try {
        const response = await featureRuntime.proposeCode({
          diagnostic: diagnostic.value,
          candidateFiles: files.value.split(',').map(x => x.trim()).filter(Boolean)
        });
        const proposal = response?.proposal || {};
        result.append(el(documentObj, 'h3', '', 'Proposal summary'), el(documentObj, 'p', '', value(proposal.summary, 'No proposal returned.')));
        const changes = Array.isArray(proposal.changes) ? proposal.changes : [];
        result.append(el(documentObj, 'p', 'feature-boundary', `${changes.length} proposed file change(s) · tests run: ${proposal.tests_run === true ? 'yes' : 'no'} · nothing applied`));
        for (const change of changes) {
          const card = el(documentObj, 'article', 'feature-code-card');
          card.append(el(documentObj, 'strong', '', value(change.path)), el(documentObj, 'p', '', value(change.reason, 'Proposed change')));
          result.append(card);
        }
      } catch (error) {
        result.append(el(documentObj, 'p', 'live-status error', 'Nova could not prepare a safe proposal.'));
        onError(error);
      } finally {
        submit.disabled = false;
        submit.textContent = 'Generate proposal';
      }
    });
    showSheet('Code Assistant', wrap);
  }

  function renderKnowledge(data) {
    const page = documentObj.querySelector('.page[data-route="knowledge"]');
    if (!page) return;
    clear(page);
    const title = el(documentObj, 'div', 'page-title-row');
    const copy = el(documentObj, 'div');
    copy.append(el(documentObj, 'h1', '', 'Knowledge Base'), el(documentObj, 'p', '', 'Trusted Nova knowledge and evidence'));
    title.append(copy);
    page.append(title);
    const stats = el(documentObj, 'div', 'feature-stat-grid');
    for (const [label, number] of [['Active',data?.active_count],['Archived',data?.archived_count],['Total',data?.count]]) {
      const card = el(documentObj, 'article', 'feature-stat');
      card.append(el(documentObj, 'strong', '', value(number, 0)), el(documentObj, 'small', '', label));
      stats.append(card);
    }
    page.append(stats);
    const search = el(documentObj, 'form', 'feature-search');
    const input = documentObj.createElement('input');
    input.placeholder = 'Search trusted knowledge…';
    input.setAttribute('aria-label', 'Search knowledge');
    const button = el(documentObj, 'button', 'small-primary', 'Search');
    button.type = 'submit';
    search.append(input, button);
    const results = el(documentObj, 'div', 'feature-stack');
    search.addEventListener('submit', async event => {
      event.preventDefault();
      const q = input.value.trim();
      if (!q) return;
      button.disabled = true;
      clear(results);
      try {
        const response = await featureRuntime.knowledgeSearch(q);
        const items = Array.isArray(response?.items) ? response.items : [];
        if (!items.length) results.append(el(documentObj, 'p', 'muted', 'No matching trusted knowledge found.'));
        for (const item of items) {
          const card = el(documentObj, 'article', 'feature-card');
          card.append(el(documentObj, 'strong', '', value(item.title)), el(documentObj, 'small', '', `${value(item.category)} · ${value(item.trust_level)}`), el(documentObj, 'p', '', value(item.snippet, 'No preview.')));
          results.append(card);
        }
      } catch (error) {
        results.append(el(documentObj, 'p', 'live-status error', 'Knowledge search is unavailable.'));
        onError(error);
      } finally { button.disabled = false; }
    });
    page.append(search, results, el(documentObj, 'p', 'feature-boundary', 'This screen exposes read-only knowledge actions. Create, update, archive, restore and delete actions are not available to this client.'));
  }

  async function loadKnowledge() {
    if (knowledgeLoaded) return;
    const page = documentObj.querySelector('.page[data-route="knowledge"]');
    if (!page) return;
    knowledgeLoaded = true;
    try {
      renderKnowledge(await featureRuntime.knowledgeSummary());
    } catch (error) {
      knowledgeLoaded = false;
      clear(page);
      page.append(el(documentObj, 'div', 'placeholder', 'Knowledge is temporarily unavailable.'));
      onError(error);
    }
  }

  function statusCard(label, primary, secondary, tone = '') {
    const card = el(documentObj, 'article', `feature-status-card${tone ? ` ${tone}` : ''}`);
    card.append(el(documentObj, 'small', '', label), el(documentObj, 'strong', '', value(primary)), el(documentObj, 'p', '', value(secondary, '')));
    return card;
  }

  async function loadControlCentre() {
    if (controlLoaded) return;
    const page = documentObj.querySelector('.page[data-route="more"]');
    if (!page) return;
    controlLoaded = true;
    clear(page);
    const head = el(documentObj, 'div', 'page-title-row');
    const copy = el(documentObj, 'div');
    copy.append(el(documentObj, 'h1', '', 'Control Centre'), el(documentObj, 'p', '', 'Read-only Nova operational intelligence'));
    head.append(copy);
    page.append(head, el(documentObj, 'p', 'muted', 'Loading current Nova health…'));
    try {
      const data = await featureRuntime.controlCentre();
      clear(page);
      page.append(head);
      const grid = el(documentObj, 'div', 'feature-status-grid');
      const metrics = data.metrics.ok ? data.metrics.data : null;
      const attention = data.attention.ok ? data.attention.data : null;
      const github = data.github.ok ? data.github.data : null;
      const learning = data.learning.ok ? data.learning.data : null;
      grid.append(
        statusCard('AI runs', metrics?.total_runs ?? 'Unavailable', metrics ? `${Math.round(Number(metrics.success_rate || 0) * 100)}% success · ${metrics.degraded_runs || 0} degraded` : data.metrics.error, metrics ? '' : 'error'),
        statusCard('Attention', attention?.counts?.needs_attention ?? 'Unavailable', attention ? `${attention.counts?.pending_work || 0} audit item(s) pending` : data.attention.error, Number(attention?.counts?.needs_attention || 0) ? 'warning' : ''),
        statusCard('Learning', learning?.count ?? 'Unavailable', learning ? `${learning.verified_count || 0} verified · ${learning.low_confidence_count || 0} low confidence` : data.learning.error),
        statusCard('GitHub broker', github ? (github.configured ? 'Configured' : 'Not configured') : 'Unavailable', github ? 'Status only in Nova Next; draft/merge actions are not exposed.' : data.github.error)
      );
      page.append(grid, el(documentObj, 'p', 'feature-boundary', 'Guardian repair execution, approvals, deployment, release, OTA, pricing authority and user/role changes are intentionally absent from this client.'));
    } catch (error) {
      controlLoaded = false;
      page.append(el(documentObj, 'p', 'live-status error', 'Control Centre data is unavailable.'));
      onError(error);
    }
  }

  function prefillChat(prompt) {
    onNavigate('chat');
    const input = documentObj.getElementById('novaNextChatInput');
    if (input) {
      input.value = prompt;
      input.focus();
    }
  }

  function bindTools() {
    const page = documentObj.querySelector('.page[data-route="tools"]');
    if (!page) return;
    for (const button of page.querySelectorAll('.list-cards button')) {
      const name = button.querySelector('strong')?.textContent?.trim() || '';
      button.addEventListener('click', () => {
        if (name === 'AI Chat') return onNavigate('chat');
        if (name === 'Image Analysis') return pickImages();
        if (name === 'Code Assistant') return openCodeProposal();
        if (name === 'Document Assistant') return prefillChat('Help me analyse and summarise a document. I will provide the content or file context.');
        if (name === 'Translate') return prefillChat('Translate the following text, preserving meaning and tone: ');
        if (name === 'Idea Generator') return prefillChat('Help me brainstorm practical ideas for: ');
        if (name === 'Data Analysis') { onNavigate('more'); return loadControlCentre(); }
        onToast(`${name || 'This tool'} is staged, but its dedicated live adapter is not connected yet.`);
      });
    }
  }

  function bind() {
    if (bound) return;
    bound = true;
    bindTools();
  }

  async function routeChanged(route) {
    if (route === 'knowledge') await loadKnowledge();
    if (route === 'more') await loadControlCentre();
  }

  return Object.freeze({ bind, routeChanged, loadKnowledge, loadControlCentre, pickImages, openCodeProposal });
}
