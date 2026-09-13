function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

export function createVoiceUi({ documentObj = globalThis.document, voiceInput, onToast = () => {} } = {}) {
  if (!documentObj) throw new TypeError('VOICE_DOCUMENT_REQUIRED');
  if (!voiceInput) throw new TypeError('VOICE_INPUT_REQUIRED');
  let button = null;
  let status = null;

  function composer() { return documentObj.getElementById('novaNextChatInput'); }

  function setState(state) {
    const listening = state === 'listening';
    if (button) button.setAttribute('aria-pressed', String(listening));
    if (status) status.textContent = listening ? 'Listening…' : '';
  }

  function appendTranscript(text) {
    const input = composer();
    if (!input) return;
    const existing = String(input.value || '').trim();
    input.value = [existing, String(text || '').trim()].filter(Boolean).join(existing ? ' ' : '');
    input.focus();
    if (status) status.textContent = 'Transcript ready — review before sending.';
  }

  function start() {
    if (!voiceInput.supported) {
      onToast('Voice input is not supported on this browser or device.', 'error');
      return false;
    }
    const ok = voiceInput.start({
      onTranscript: appendTranscript,
      onState: setState,
      onError: () => onToast('Voice input stopped. You can continue typing.', 'error')
    });
    if (!ok && voiceInput.isActive()) voiceInput.stop();
    return ok;
  }

  function bind() {
    if (button) return;
    const send = documentObj.getElementById('novaNextChatSend');
    const wrapper = send?.parentElement;
    if (!send || !wrapper) return;
    button = el(documentObj, 'button', 'voice-input-button', '◉');
    button.type = 'button';
    button.setAttribute('aria-label', 'Voice input');
    button.setAttribute('aria-pressed', 'false');
    button.disabled = !voiceInput.supported;
    button.title = voiceInput.supported ? 'Start voice input' : 'Voice input is not supported on this browser or device';
    button.addEventListener('click', start);
    status = el(documentObj, 'span', 'sr-only');
    status.id = 'novaNextVoiceStatus';
    status.setAttribute('role', 'status');
    status.setAttribute('aria-live', 'polite');
    wrapper.insertBefore(button, send);
    wrapper.after(status);
  }

  function stop() {
    voiceInput.stop();
    setState('idle');
  }

  function destroy() {
    voiceInput.destroy();
    button?.remove();
    status?.remove();
    button = null;
    status = null;
  }

  return Object.freeze({ bind, start, stop, destroy, appendTranscript });
}
