// Client for the Darija Translator REST service, shared by the side panel and options page.

export const DEFAULT_SETTINGS = {
  serverUrl: 'http://localhost:8080',
  username: 'student',
  password: 'darija2026',
};

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export async function getSettings() {
  return chrome.storage.local.get(DEFAULT_SETTINGS);
}

/** HTTP Basic header; encodes UTF-8 so non-ASCII passwords work too. */
export function basicAuth(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = '';
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return `Basic ${btoa(binary)}`;
}

function baseUrl(serverUrl) {
  return serverUrl.trim().replace(/\/+$/, '');
}

async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function friendlyError(status, body) {
  if (status === 401) return 'Wrong username or password (check Settings).';
  if (status === 429) return 'The model quota is exhausted. Try again in a minute.';
  return body?.message || `Server error (HTTP ${status}).`;
}

/**
 * POST /api/translate
 * @returns {Promise<{translation:string, script:string, provider:string, model:string, cached:boolean, durationMs:number}>}
 */
export async function translate(text, { sourceLanguage = 'auto', script = 'arabic', signal, settings } = {}) {
  const s = settings || (await getSettings());
  let response;
  try {
    response = await fetch(`${baseUrl(s.serverUrl)}/api/translate`, {
      method: 'POST',
      // 'omit' avoids the browser's own Basic-auth popup on 401; we send the header ourselves.
      credentials: 'omit',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        Authorization: basicAuth(s.username, s.password),
      },
      body: JSON.stringify({ text, sourceLanguage, script }),
      signal,
    });
  } catch (e) {
    if (e.name === 'AbortError') throw e;
    throw new ApiError(0, `Cannot reach ${s.serverUrl}. Is the server running?`);
  }
  const body = await readJson(response);
  if (!response.ok) throw new ApiError(response.status, friendlyError(response.status, body));
  return body;
}

/** GET /api/health (public). */
export async function health(serverUrl) {
  let response;
  try {
    response = await fetch(`${baseUrl(serverUrl)}/api/health`, { credentials: 'omit' });
  } catch {
    throw new ApiError(0, `Cannot reach ${serverUrl}.`);
  }
  const body = await readJson(response);
  if (!response.ok) throw new ApiError(response.status, friendlyError(response.status, body));
  return body;
}
