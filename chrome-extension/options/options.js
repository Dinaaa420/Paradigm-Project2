import { DEFAULT_SETTINGS, getSettings, health, translate } from '../shared/api.js';

const form = document.getElementById('form');
const status = document.getElementById('status');
const fields = ['serverUrl', 'username', 'password'];

function setStatus(message, isError = false) {
  status.textContent = message;
  status.classList.toggle('error', isError);
}

function readForm() {
  return Object.fromEntries(fields.map((f) => [f, document.getElementById(f).value.trim()]));
}

/** Servers other than localhost need a host permission, requested at runtime (optional_host_permissions). */
async function ensureHostPermission(serverUrl) {
  const url = new URL(serverUrl);
  if (['localhost', '127.0.0.1'].includes(url.hostname)) return true;
  const origins = [`${url.protocol}//${url.hostname}/*`];
  if (await chrome.permissions.contains({ origins })) return true;
  return chrome.permissions.request({ origins });
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const settings = readForm();
  if (!(await ensureHostPermission(settings.serverUrl))) {
    setStatus('Permission to contact that server was refused.', true);
    return;
  }
  await chrome.storage.local.set(settings);
  setStatus('Saved ✓');
});

document.getElementById('testBtn').addEventListener('click', async () => {
  const settings = readForm();
  setStatus('Testing…');
  try {
    if (!(await ensureHostPermission(settings.serverUrl))) throw new Error('Host permission refused.');
    const h = await health(settings.serverUrl);
    const r = await translate('Hello', { settings });
    setStatus(`✓ Connected (${h.provider} / ${h.model}). "Hello" → ${r.translation}`);
  } catch (err) {
    setStatus(`✗ ${err.message}`, true);
  }
});

(async () => {
  const s = await getSettings();
  for (const f of fields) document.getElementById(f).value = s[f] ?? DEFAULT_SETTINGS[f];
})();
