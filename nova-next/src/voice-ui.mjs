import { createVoiceInput } from './voice-input.mjs';

function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

export function createVoiceUi({ documentObj = globalThis.document, windowObj = globalThis.window, onToast = () => {} } = {}) {
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');
  let bound = false;
  let voice = null;
  let button = null;
  let status = null;

  function chatInput() {
    return documentObj.getElementById('novaNextChatInput');
  }

  function setButtonState(state) {
    if (!button) return;
    const listening = state === 'listening';
    button.setAttribute('aria-pressed', String(listening));
    button.setAttribute('aria-label', listening ? 'Stop voice input' : 'Voice input');
    if (status) {
      status.textContent = listening
        ? 'Listening…'
        : state.startsWith('error:')
          ? 'Voice input stopped. You can continue typing.'
          : '';
    }
    if (state.startsWith('error:')) onToast('Voice input could not continue. You can keep typing instead.', 'error');
  }

  function fillTranscript(transcript) {
    const input = chatInput();
    if (!input) {
      onToast('Chat input is unavailable.', 'error');
      return;
    }
    const incoming = String(transcript || '').trim();
    if (!incoming) return;
    const existing = String(input.value || '').trim();
    input.value = [existing, incoming].filter(Boolean).join(existing ? ' ' : '');
    input.focus();
    if (windowObj?.Event) input.dispatchEvent(new windowObj.Event('input', { bubbles: true }));
    if (status) status.textContent = 'Transcript ready — review before sending.';
    onToast('Voice transcript added to Chat. Review it before sending.');
  }

  function bind() {
    if (bound) return;
    const send = documentObj.getElementById('novaNextChatSend');
    const wrapper = send?.parentElement;
    if (!send || !wrapper) return;
    bound = true;

    button = el(documentObj, 'button', 'voice-input-button', '◉');
    button.type = 'button';
    button.setAttribute('aria-label', 'Voice input');
    button.setAttribute('aria-pressed', 'false');

    status = el(documentObj, 'span', 'sr-only');
    status.id = 'novaNextVoiceStatus';
    status.setAttribute('role', 'status');
    status.setAttribute('aria-live', 'polite');

    voice = createVoiceInput({
      RecognitionCtor: windowObj?.SpeechRecognition || windowObj?.webkitSpeechRecognition,
      onTranscript: fillTranscript,
      onStatus: setButtonState
    });

    if (!voice.supported) {
      button.disabled = true;
      button.setAttribute('aria-label', 'Voice input unavailable');
      button.title = 'Voice input is not supported on this browser or device';
    } else {
      button.addEventListener('click', () => {
        if (button.getAttribute('aria-pressed') === 'true') voice.stop();
        else voice.start();
      });
    }

    wrapper.insertBefore(button, send);
    wrapper.after(status);
  }

  function stop() {
    if (!voice?.supported) return false;
    try { voice.stop(); } catch {}
    setButtonState('idle');
    return true;
  }

  function destroy() {
    stop();
    button?.remove();
    status?.remove();
    button = null;
    status = null;
    voice = null;
    bound = false;
  }

  return Object.freeze({ bind, stop, destroy });
}
