export function createVoiceInput({ RecognitionCtor = globalThis.SpeechRecognition || globalThis.webkitSpeechRecognition, onTranscript = () => {}, onStatus = () => {} } = {}) {
  if (typeof onTranscript !== 'function') throw new TypeError('TRANSCRIPT_HANDLER_REQUIRED');
  if (typeof onStatus !== 'function') throw new TypeError('STATUS_HANDLER_REQUIRED');
  if (typeof RecognitionCtor !== 'function') {
    return Object.freeze({ supported: false, start: () => false, stop: () => false });
  }

  const recognition = new RecognitionCtor();
  recognition.lang = 'en-AU';
  recognition.interimResults = false;
  recognition.continuous = false;

  recognition.onstart = () => onStatus('listening');
  recognition.onend = () => onStatus('idle');
  recognition.onerror = event => onStatus(event?.error ? `error:${event.error}` : 'error');
  recognition.onresult = event => {
    const transcript = String(event?.results?.[0]?.[0]?.transcript || '').trim();
    if (transcript) onTranscript(transcript);
  };

  return Object.freeze({
    supported: true,
    start() {
      recognition.start();
      return true;
    },
    stop() {
      recognition.stop();
      return true;
    }
  });
}
