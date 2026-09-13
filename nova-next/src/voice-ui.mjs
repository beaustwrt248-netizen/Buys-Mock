import { createVoiceInput } from './voice-input.mjs';

export function createVoiceUi({ documentObj = globalThis.document, windowObj = globalThis.window, onToast = () => {} } = {}) {
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');
  let bound = false;
  let voice = null;

  function chatInput() {
    return documentObj.getElementById('novaNextChatInput')
      || documentObj.querySelector('.page[data-route="chat"] .composer input');
  }

  function voiceButton() {
    return documentObj.querySelector('.page[data-route="chat"] [aria-label="Voice input"]');
  }

  function setButtonState(state) {
    const button = voiceButton();
    if (!button) return;
    const listening = state === 'listening';
    button.setAttribute('aria-pressed', String(listening));
    button.setAttribute('aria-label', listening ? 'Stop voice input' : 'Voice input');
    if (state.startsWith('error:')) onToast('Voice input could not continue. You can keep typing instead.', 'error');
  }

  function fillTranscript(transcript) {
    const input = chatInput();
    if (!input) {
      onToast('Chat input is unavailable.', 'error');
      return;
    }
    input.value = String(transcript || '').trim();
    input.focus();
    if (windowObj?.Event) input.dispatchEvent(new windowObj.Event('input', { bubbles: true }));
    onToast('Voice transcript added to Chat. Review it before sending.');
  }

  function bind() {
    if (bound) return;
    bound = true;
    const button = voiceButton();
    if (!button) return;
    voice = createVoiceInput({
      RecognitionCtor: windowObj?.SpeechRecognition || windowObj?.webkitSpeechRecognition,
      onTranscript: fillTranscript,
      onStatus: setButtonState
    });
    if (!voice.supported) {
      button.disabled = true;
      button.setAttribute('aria-label', 'Voice input unavailable');
      return;
    }
    button.setAttribute('aria-pressed', 'false');
    button.addEventListener('click', () => {
      if (button.getAttribute('aria-pressed') === 'true') voice.stop();
      else voice.start();
    });
  }

  return Object.freeze({ bind });
}
