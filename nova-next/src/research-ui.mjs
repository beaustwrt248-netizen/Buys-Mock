const RESEARCH_PROMPT = 'Research this using current, relevant sources. Cite source links and dates for material claims. Separate verified facts, uncertainty, and recommendations. If credible sources conflict, explain the conflict instead of hiding it: ';

export function createResearchUi({ documentObj = globalThis.document, onNavigate = () => {}, onToast = () => {} } = {}) {
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');
  let bound = false;

  function chatInput() {
    return documentObj.getElementById('novaNextChatInput')
      || documentObj.querySelector('.page[data-route="chat"] .composer input');
  }

  function prefill() {
    onNavigate('chat');
    const input = chatInput();
    if (!input) {
      onToast('Chat input is unavailable.', 'error');
      return false;
    }
    input.value = RESEARCH_PROMPT;
    input.focus();
    onToast('Evidence-first research prompt is ready. Add your topic, then send when ready.');
    return true;
  }

  function bind() {
    if (bound) return;
    const button = documentObj.querySelector('[data-tool="research"]');
    if (!button) return;
    bound = true;
    button.addEventListener('click', event => {
      event.preventDefault();
      event.stopImmediatePropagation();
      prefill();
    });
  }

  return Object.freeze({ bind, prefill });
}

export { RESEARCH_PROMPT };
