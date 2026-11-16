// Read-aloud (text-to-speech) with expo-speech, which uses the phone's own voices.
import * as Speech from 'expo-speech';

let cachedVoices = null;

async function pickVoice(script) {
  if (!cachedVoices) {
    try {
      cachedVoices = await Speech.getAvailableVoicesAsync();
    } catch {
      cachedVoices = [];
    }
  }
  // A true Darija (ar-MA) voice is rare: fall back to any Arabic voice.
  // Latin-script Darija sounds closest with a French voice.
  const prefs = script === 'latin' ? ['fr-MA', 'fr-FR', 'fr'] : ['ar-MA', 'ar-DZ', 'ar-SA', 'ar'];
  for (const p of prefs) {
    const v = cachedVoices.find((voice) => voice.language?.toLowerCase().replace('_', '-').startsWith(p.toLowerCase()));
    if (v) return v;
  }
  return null;
}

export async function speak(text, script, { onDone } = {}) {
  await Speech.stop();
  const voice = await pickVoice(script);
  Speech.speak(text, {
    language: voice?.language || (script === 'latin' ? 'fr-FR' : 'ar-MA'),
    voice: voice?.identifier,
    rate: 0.95,
    onDone,
    onStopped: onDone,
    onError: onDone,
  });
}

export function stop() {
  return Speech.stop();
}
