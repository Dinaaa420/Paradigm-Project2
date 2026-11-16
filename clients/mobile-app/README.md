# React Native mobile app (Expo)

Translate text into Darija from your phone: language chips, Arabic/Latin script switch,
**Paste & translate**, **Read aloud** (phone's text-to-speech), copy, and history.
The password is kept in the phone's secure keystore (`expo-secure-store`).

## Run it

```bash
npm install
npx expo start
```

Then:

- **Real phone:** install **Expo Go** from the Play Store / App Store and scan the QR code.
  Phone and computer must be on the same Wi-Fi.
- **Android emulator:** press `a`.
- **Browser:** press `w`.

## Point it at the server

Open ⚙︎ Settings in the app:

| Where the app runs | Server URL |
|---|---|
| Android emulator | `http://10.0.2.2:8080` (default) |
| iOS simulator / web | `http://localhost:8080` |
| Real phone | `http://<your-computer-IP>:8080`. Find it with `ipconfig` (Windows) or `ip a` / `ifconfig`. |

Then tap **Test connection**. If a real phone can't connect, allow port 8080 through your computer's firewall.

> Plain `http://` works in Expo Go. For a standalone release build, serve the API over HTTPS,
> or enable cleartext traffic with `expo-build-properties`.

## Files

| File | Role |
|---|---|
| `App.js` | Loads settings, switches between the two screens |
| `src/TranslateScreen.js` | Main UI |
| `src/SettingsScreen.js` | Server URL and credentials, with a connection test |
| `src/api.js` | `translate()` / `health()` REST calls with HTTP Basic auth |
| `src/storage.js` | AsyncStorage + SecureStore persistence |
| `src/speech.js` | Read aloud with `expo-speech` |
