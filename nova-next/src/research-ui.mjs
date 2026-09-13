import { normaliseKnowledgeContext } from './research-metadata.mjs';

const PRESETS = Object.freeze({
  compare: 'Compare the following options using evidence. Separate verified facts, uncertainty, assumptions and recommendations: ',
  investigate: 'Investigate the following question using evidence. Clearly separate confirmed facts, uncertainty, conflicting evidence and next checks: ',
  summarise: 'Summarise the following material using evidence. Distinguish source-backed facts from interpretation and missing context: ',
  scenario: 'Analyse this scenario using evidence. Separate facts, assumptions, risks, options and recommendations: '
});

function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

export function createResearchUi({ documentObj = globalThis.document, onNavigate = () => {}, getComposer = () => null, onToast = () => {} } = {}) {
  if (!documentObj) throw new TypeError('RESEARCH_DOCUMENT_REQUIRED');

  function close() { documentObj.querySelector('.research-sheet-backdrop')?.remove(); }

  function openPreset(id) {
    const prompt = PRESETS[id];
    if (!prompt) return false;
    onNavigate('chat');
    const input = getComposer();
    if (input) {
      input.value = prompt;
      input.focus();
      onToast('Research prompt prepared — review before sending.');
    }
    close();
    return true;
  }

  function open() {
    close();
    const backdrop = el(documentObj, 'div', 'feature-sheet-backdrop research-sheet-backdrop');
    const sheet = el(documentObj, 'section', 'feature-sheet');
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    sheet.setAttribute('aria-label', 'Research');
    const head = el(documentObj, 'div', 'feature-sheet-head');
    head.append(el(documentObj, 'h2', '', 'Research'));
    const closeButton = el(documentObj, 'button', 'icon-button', '×');
    closeButton.type = 'button';
    closeButton.setAttribute('aria-label', 'Close Research');
    closeButton.addEventListener('click', close);
    head.append(closeButton);
    const intro = el(documentObj, 'p', 'feature-boundary', 'Research uses the guarded Nova orchestrator. Evidence is advisory and never triggers protected actions. Choose a starting mode; you review the prompt before sending.');
    const grid = el(documentObj, 'div', 'feature-stack');
    for (const [id, label] of [['compare','Compare options'],['investigate','Investigate'],['summarise','Summarise'],['scenario','Scenario analysis']]) {
      const button = el(documentObj, 'button', 'secondary-button', label);
      button.type = 'button';
      button.dataset.researchPreset = id;
      button.addEventListener('click', () => openPreset(id));
      grid.append(button);
    }
    sheet.append(head, intro, grid);
    backdrop.append(sheet);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) close(); });
    documentObj.body.append(backdrop);
    closeButton.focus({ preventScroll: true });
  }

  function renderEvidence(result) {
    documentObj.querySelector('.research-evidence')?.remove();
    const meta = normaliseKnowledgeContext(result?.knowledge_context || result?.knowledgeContext);
    if (!meta.used && !meta.degraded) return meta;
    const messages = documentObj.getElementById('novaNextChatMessages');
    if (!messages) return meta;
    const panel = el(documentObj, 'aside', `research-evidence${meta.degraded ? ' warning' : ''}`);
    panel.setAttribute('aria-label', 'Research evidence status');
    panel.append(
      el(documentObj, 'strong', '', meta.used ? `Evidence used · ${meta.count}` : 'Evidence unavailable'),
      el(documentObj, 'small', '', meta.semantic ? 'Semantic retrieval' : 'Keyword/fallback retrieval')
    );
    if (meta.degraded) panel.append(el(documentObj, 'p', '', 'Knowledge retrieval degraded; review the answer and evidence carefully.'));
    if (meta.items.length) {
      const details = documentObj.createElement('details');
      const summary = el(documentObj, 'summary', '', 'View evidence');
      const list = el(documentObj, 'div', 'feature-stack');
      for (const item of meta.items) {
        const card = el(documentObj, 'article', 'feature-card');
        card.append(el(documentObj, 'strong', '', item.title), el(documentObj, 'small', '', [item.sourceLabel, item.trustLevel, item.confidence === null ? '' : `${Math.round(item.confidence * 100)}%`].filter(Boolean).join(' · ')));
        list.append(card);
      }
      details.append(summary, list);
      panel.append(details);
    }
    messages.after(panel);
    return meta;
  }

  return Object.freeze({ open, close, openPreset, renderEvidence });
}
