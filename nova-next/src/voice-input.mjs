export function createVoiceInput({ SpeechRecognitionCtor = globalThis.SpeechRecognition || globalThis.webkitSpeechRecognition } = {}) {
  let recognition = null;
  let active = false;
  let callbacks = {};
  const supported = typeof SpeechRecognitionCtor === 'function';

  function stop() {
    if (!recognition) return false;
    active = false;
    try { recognition.stop(); } catch {}
    return true;
  }

  function destroy() {
    active = false;
    if (recognition) {
      try { recognition.abort?.(); } catch {}
      recognition.onresult = null;
      recognition.onerror = null;
      recognition.onend = null;
    }
    recognition = null;
  }

  function start({ onTranscript = () => {}, onState = () => {}, onError = () => {} } = {}) {
    if (!supported || active) return false;
    callbacks = { onTranscript, onState, onError };
    recognition = new SpeechRecognitionCtor();
    recognition.continuous = false;
    recognition.interimResults = true;
    recognition.lang = 'en-AU';
    recognition.onresult = event => {
      const pieces = [];
      for (const result of Array.from(event?.results || [])) {
        const text = String(result?.[0]?.transcript || '').trim();
        if (text) pieces.push(text);
      }
      const transcript = pieces.join(' ').trim();
      if (transcript) callbacks.onTranscript(transcript);
    };
    recognition.onerror = event => {
      active = false;
      callbacks.onState('error');
      callbacks.onError(event?.error || 'VOICE_RECOGNITION_FAILED');
    };
    recognition.onend = () => {
      active = false;
      callbacks.onState('idle');
    };
    active = true;
    callbacks.onState('listening');
    recognition.start();
    return true;
  }

  return Object.freeze({ supported, start, stop, destroy, isActive: () => active });
}
