// Service worker (Manifest V3): context menu + side panel wiring.

const MENU_TRANSLATE = 'darija-translate-selection';
const MENU_OPEN = 'darija-open-panel';

chrome.runtime.onInstalled.addListener(async ({ reason }) => {
  chrome.contextMenus.removeAll(() => {
    chrome.contextMenus.create({
      id: MENU_TRANSLATE,
      title: 'Translate "%s" to Darija',
      contexts: ['selection'],
    });
    chrome.contextMenus.create({
      id: MENU_OPEN,
      title: 'Open Darija Translator',
      contexts: ['page'],
    });
  });

  // Clicking the toolbar icon also opens the side panel.
  await chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });

  if (reason === 'install') {
    chrome.runtime.openOptionsPage();
  }
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
  // sidePanel.open() must run synchronously inside the user gesture, so call it first, no await before it.
  chrome.sidePanel.open({ windowId: tab.windowId });

  if (info.menuItemId === MENU_TRANSLATE && info.selectionText) {
    // The panel picks this up (on load, or live through storage.onChanged) and translates immediately.
    chrome.storage.session.set({
      pendingSelection: { text: info.selectionText, source: 'context-menu', at: Date.now() },
    });
  }
});

// Live selection from the content script: forward only while the panel wants it.
chrome.runtime.onMessage.addListener((message) => {
  if (message?.type === 'selection' && typeof message.text === 'string') {
    chrome.storage.session.set({
      liveSelection: { text: message.text, source: 'live', at: Date.now() },
    });
  }
});
