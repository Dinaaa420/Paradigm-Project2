// Grants microphone access to the extension's origin; the side panel then inherits it.
const status = document.getElementById('status');
navigator.mediaDevices.getUserMedia({ audio: true })
  .then((stream) => {
    stream.getTracks().forEach((t) => t.stop());
    status.textContent = '✓ Microphone allowed. You can close this tab.';
    setTimeout(() => window.close(), 1500);
  })
  .catch(() => {
    status.textContent = 'Microphone was blocked. Allow it from the 🔒 icon in the address bar, then reload this page.';
    status.classList.add('error');
  });
