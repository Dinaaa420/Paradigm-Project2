// Client for the Darija Translator REST service.

const B64 = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

/** Base64 of the UTF-8 bytes of a string (works in every JS engine, no btoa needed). */
function base64Utf8(text) {
  const bytes = [];
  for (const ch of text) {
    let c = ch.codePointAt(0);
    if (c < 0x80) bytes.push(c);
    else if (c < 0x800) bytes.push(0xc0 | (c >> 6), 0x80 | (c & 63));
    else if (c < 0x10000) bytes.push(0xe0 | (c >> 12), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
    else bytes.push(0xf0 | (c >> 18), 0x80 | ((c >> 12) & 63), 0x80 | ((c >> 6) & 63), 0x80 | (c & 63));
  }
  let out = '';
  for (let i = 0; i < bytes.length; i += 3) {
    const [a, b = 0, c = 0] = [bytes[i], bytes[i + 1], bytes[i + 2]];
    const n = (a << 16) | (b << 8) | c;
    out += B64[(n >> 18) & 63] + B64[(n >> 12) & 63]
      + (i + 1 < bytes.length ? B64[(n >> 6) & 63] : '=')
      + (i + 2 < bytes.length ? B64[n & 63] : '=');
  }
  return out;
}

export function basicAuth(username, password) {
  return `Basic ${base64Utf8(`${username}:${password}`)}`;
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const base = (url) => url.trim().replace(/\/+$/, '');

async function call(url, options, timeoutMs = 60000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  let response;
  try {
    response = await fetch(url, { ...options, signal: controller.signal });
  } catch (e) {
    throw new ApiError(0, e.name === 'AbortError'
      ? 'The server took too long to answer.'
      : `Cannot reach ${url.split('/api')[0]}. Check the server URL in Settings.`);
  } finally {
    clearTimeout(timer);
  }
  let body = null;
  try {
    body = await response.json();
  } catch {
    // non-JSON body
  }
  if (response.status === 401) throw new ApiError(401, 'Wrong username or password (see Settings).');
  if (!response.ok) throw new ApiError(response.status, body?.message || `Server error (HTTP ${response.status}).`);
  return body;
}

/** POST /api/translate */
export function translate(settings, text, { sourceLanguage = 'auto', script = 'arabic' } = {}) {
  return call(`${base(settings.serverUrl)}/api/translate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json',
      Authorization: basicAuth(settings.username, settings.password),
    },
    body: JSON.stringify({ text, sourceLanguage, script }),
  });
}

/** GET /api/health (public) */
export function health(serverUrl) {
  return call(`${base(serverUrl)}/api/health`, { headers: { Accept: 'application/json' } }, 10000);
}
