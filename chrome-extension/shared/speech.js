// Speech helpers: text-to-speech (read aloud) and speech-to-text (voice input),
// both using built-in browser APIs, so no extra server or key is needed.

/** Speech-recognition locale for each source language offered in the UI. */
export const RECOGNITION_LANG = {
  auto: navigator.language || 'en-US',
  English: 'en-US',
  French: 'fr-FR',
  Spanish: 'es-ES',
  German: 'de-DE',
  'Modern Standard Arabic': 'ar-SA',
  Italian: 'it-IT',
};

let voicesReady = null;

function loadVoices() {
  if (!voicesReady) {
    voicesReady = new Promise((resolve) => {
      const voices = speechSynthesis.getVoices();
      if (voices.length) return resolve(voices);
      speechSynthesis.addEventListener('voiceschanged', () => resolve(speechSynthesis.getVoices()), { once: true });
      setTimeout(() => resolve(speechSynthesis.getVoices()), 1500);
    });
  }
  return voicesReady;
}

/**
 * Picks the best available voice. There is rarely a true Darija (ar-MA) voice, so fall back to any
 * Arabic voice; Latin-script Darija (Arabizi) sounds closest with a French voice.
 */
async function pickVoice(script) {
  const voices = await loadVoices();
  const prefs = script === 'latin' ? ['fr-MA', 'fr-FR', 'fr'] : ['ar-MA', 'ar-DZ', 'ar-TN', 'ar-SA', 'ar'];
  for (const p of prefs) {
    const v = voices.find((voice) => voice.lang?.toLowerCase().startsWith(p.toLowerCase()));
    if (v) return v;
  }
  return null;
}

export function isSpeaking() {
  return speechSynthesis.speaking;
}

export function stopSpeaking() {
  speechSynthesis.cancel();
}

/** Reads text aloud. Resolves when finished. */
export async function speak(text, script = 'arabic', { rate = 0.95 } = {}) {
  stopSpeaking();
  const utterance = new SpeechSynthesisUtterance(text);
  const voice = await pickVoice(script);
  if (voice) utterance.voice = voice;
  utterance.lang = voice?.lang || (script === 'latin' ? 'fr-FR' : 'ar-MA');
  utterance.rate = rate;
  return new Promise((resolve, reject) => {
    utterance.onend = () => resolve();
    utterance.onerror = (e) => (e.error === 'interrupted' || e.error === 'canceled' ? resolve() : reject(e));
    speechSynthesis.speak(utterance);
  });
}

export function speechRecognitionSupported() {
  return 'webkitSpeechRecognition' in window || 'SpeechRecognition' in window;
}

/**
 * Starts listening; calls onInterim with partial text and resolves with the final transcript.
 * Returns { promise, stop }.
 */
export function listen(lang, onInterim) {
  const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  const recognition = new Recognition();
  recognition.lang = lang;
  recognition.interimResults = true;
  recognition.continuous = false;

  let finalText = '';
  const promise = new Promise((resolve, reject) => {
    recognition.onresult = (event) => {
      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const r = event.results[i];
        if (r.isFinal) finalText += r[0].transcript;
        else interim += r[0].transcript;
      }
      onInterim?.((finalText + interim).trim());
    };
    recognition.onerror = (e) => reject(e.error || 'speech-error');
    recognition.onend = () => resolve(finalText.trim());
  });
  recognition.start();
  return { promise, stop: () => recognition.stop() };
}
