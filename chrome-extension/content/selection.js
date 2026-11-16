// Content script: reports text the user selects on the page, so the side panel can
// auto-paste it when "Auto-paste selection" is on. Tiny on purpose: no DOM changes.
(() => {
  let last = '';
  let timer = null;

  function report() {
    const text = (window.getSelection()?.toString() || '').trim();
    if (text.length < 2 || text === last || text.length > 5000) return;
    last = text;
    try {
      chrome.runtime.sendMessage({ type: 'selection', text });
    } catch {
      // Extension was reloaded; this old content script is orphaned. Ignore.
    }
  }

  document.addEventListener('mouseup', () => {
    clearTimeout(timer);
    timer = setTimeout(report, 250);
  });
  document.addEventListener('keyup', (e) => {
    if (e.shiftKey || e.key === 'a') {
      clearTimeout(timer);
      timer = setTimeout(report, 400);
    }
  });
})();
