import { translate } from '../shared/api.js';
import {
  RECOGNITION_LANG, speak, stopSpeaking, isSpeaking, listen, speechRecognitionSupported,
} from '../shared/speech.js';

const $ = (id) => document.getElementById(id);
const el = {
  sourceLang: $('sourceLang'), sourceText: $('sourceText'), charCount: $('charCount'),
  livePaste: $('livePaste'), translateBtn: $('translateBtn'), output: $('output'), meta: $('meta'),
  speakBtn: $('speakBtn'), copyBtn: $('copyBtn'), autoSpeak: $('autoSpeak'), micBtn: $('micBtn'),
  status: $('status'), history: $('history'), clearHistory: $('clearHistory'), settingsBtn: $('settingsBtn'),
};

const HISTORY_MAX = 20;
let current = null;          // last successful translation
let inFlight = null;         // AbortController of the running request
let recognition = null;      // active speech recognition

const script = () => document.querySelector('input[name=script]:checked').value;

function setStatus(message, isError = false) {
  el.status.textContent = message || '';
  el.status.classList.toggle('error', isError);
}

function updateCount() {
  el.charCount.textContent = `${el.sourceText.value.length} / 5000`;
}

function showResult(result) {
  current = result;
  el.output.textContent = result.translation;
  el.output.dir = result.script === 'arabic' ? 'rtl' : 'ltr';
  el.output.lang = result.script === 'arabic' ? 'ary' : 'ary-Latn';
  el.meta.textContent = `${result.provider} · ${result.cached ? 'cached' : `${result.durationMs} ms`}`;
  el.speakBtn.disabled = false;
  el.copyBtn.disabled = false;
}

async function doTranslate({ speakAfter = false } = {}) {
  const text = el.sourceText.value.trim();
  if (!text) {
    setStatus('Type or select some text first.');
    return;
  }
  inFlight?.abort();
  inFlight = new AbortController();

  el.translateBtn.disabled = true;
  el.output.classList.add('loading');
  setStatus('Translating…');
  try {
    const result = await translate(text, {
      sourceLanguage: el.sourceLang.value, script: script(), signal: inFlight.signal,
    });
    showResult(result);
    setStatus('');
    saveHistory(text, result);
    if (speakAfter || el.autoSpeak.checked) readAloud();
  } catch (e) {
    if (e.name !== 'AbortError') setStatus(e.message, true);
  } finally {
    el.translateBtn.disabled = false;
    el.output.classList.remove('loading');
  }
}

/** Puts text into the box and translates it right away ("copy & paste" done for the user). */
function pasteAndTranslate(text) {
  if (!text) return;
  el.sourceText.value = text.slice(0, 5000);
  updateCount();
  doTranslate();
}

async function readAloud() {
  if (!current) return;
  if (isSpeaking()) {
    stopSpeaking();
    el.speakBtn.textContent = '🔊 Read aloud';
    return;
  }
  el.speakBtn.textContent = '⏹ Stop';
  try {
    await speak(current.translation, current.script);
  } catch {
    setStatus('No voice available for reading aloud on this computer.', true);
  } finally {
    el.speakBtn.textContent = '🔊 Read aloud';
  }
}

// ---- Voice-to-voice: speak → transcript → translate → read the Darija aloud -------------------
async function toggleMic() {
  if (recognition) {
    recognition.stop();
    return;
  }
  if (!speechRecognitionSupported()) {
    setStatus('Speech recognition is not supported in this browser.', true);
    return;
  }
  const lang = RECOGNITION_LANG[el.sourceLang.value] || 'en-US';
  el.micBtn.classList.add('listening');
  setStatus(`Listening (${lang})… speak now`);
  try {
    recognition = listen(lang, (partial) => { el.sourceText.value = partial; updateCount(); });
    const transcript = await recognition.promise;
    if (transcript) {
      el.sourceText.value = transcript;
      updateCount();
      await doTranslate({ speakAfter: true });
    } else {
      setStatus('Didn’t catch that. Try again.');
    }
  } catch (err) {
    if (err === 'not-allowed' || err === 'service-not-allowed') {
      setStatus('Microphone access needed: a tab opened to grant it once.', true);
      chrome.tabs.create({ url: chrome.runtime.getURL('permission/mic.html') });
    } else {
      setStatus(`Voice input error: ${err}`, true);
    }
  } finally {
    recognition = null;
    el.micBtn.classList.remove('listening');
  }
}

// ---- History ---------------------------------------------------------------------------------
async function saveHistory(text, result) {
  const { history = [] } = await chrome.storage.local.get('history');
  const entry = { text, translation: result.translation, script: result.script, at: Date.now() };
  const next = [entry, ...history.filter((h) => !(h.text === text && h.script === result.script))].slice(0, HISTORY_MAX);
  await chrome.storage.local.set({ history: next });
  renderHistory(next);
}

function renderHistory(history) {
  el.history.replaceChildren(...history.map((h) => {
    const li = document.createElement('li');
    const src = document.createElement('div');
    src.className = 'src';
    src.textContent = h.text;
    const dst = document.createElement('div');
    dst.className = 'dst';
    dst.dir = h.script === 'arabic' ? 'rtl' : 'ltr';
    dst.textContent = h.translation;
    li.append(src, dst);
    li.title = new Date(h.at).toLocaleString();
    li.addEventListener('click', () => {
      el.sourceText.value = h.text;
      updateCount();
      showResult({ translation: h.translation, script: h.script, provider: 'history', cached: true });
    });
    return li;
  }));
}

// ---- Wiring ----------------------------------------------------------------------------------
el.translateBtn.addEventListener('click', () => doTranslate());
el.sourceText.addEventListener('input', updateCount);
el.sourceText.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) doTranslate();
});
el.speakBtn.addEventListener('click', readAloud);
el.copyBtn.addEventListener('click', async () => {
  if (!current) return;
  await navigator.clipboard.writeText(current.translation);
  setStatus('Copied to clipboard.');
});
el.micBtn.addEventListener('click', toggleMic);
el.settingsBtn.addEventListener('click', () => chrome.runtime.openOptionsPage());
el.clearHistory.addEventListener('click', async () => {
  await chrome.storage.local.set({ history: [] });
  renderHistory([]);
});
document.querySelectorAll('input[name=script]').forEach((r) => r.addEventListener('change', () => {
  chrome.storage.local.set({ script: script() });
  if (el.sourceText.value.trim() && current) doTranslate();
}));
for (const key of ['livePaste', 'autoSpeak']) {
  el[key].addEventListener('change', () => chrome.storage.local.set({ [key]: el[key].checked }));
}
el.sourceLang.addEventListener('change', () => chrome.storage.local.set({ sourceLang: el.sourceLang.value }));

// Text sent by the context menu (or by the content script when auto-paste is on).
chrome.storage.session.onChanged.addListener((changes) => {
  if (changes.pendingSelection?.newValue) {
    pasteAndTranslate(changes.pendingSelection.newValue.text);
    chrome.storage.session.remove('pendingSelection');
  }
  if (changes.liveSelection?.newValue && el.livePaste.checked) {
    pasteAndTranslate(changes.liveSelection.newValue.text);
  }
});

async function init() {
  const prefs = await chrome.storage.local.get({
    script: 'arabic', livePaste: false, autoSpeak: false, sourceLang: 'auto', history: [],
  });
  document.getElementById(prefs.script === 'latin' ? 'scriptLatin' : 'scriptArabic').checked = true;
  el.livePaste.checked = prefs.livePaste;
  el.autoSpeak.checked = prefs.autoSpeak;
  el.sourceLang.value = prefs.sourceLang;
  renderHistory(prefs.history);
  updateCount();
  if (!speechRecognitionSupported()) el.micBtn.hidden = true;

  // The panel may have just been opened by the context menu: translate what was selected.
  const { pendingSelection } = await chrome.storage.session.get('pendingSelection');
  if (pendingSelection && Date.now() - pendingSelection.at < 15000) {
    pasteAndTranslate(pendingSelection.text);
  }
  await chrome.storage.session.remove('pendingSelection');
}

init();
