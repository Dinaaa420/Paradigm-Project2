// Settings + history persistence. The password goes to the OS keychain/keystore (expo-secure-store);
// everything else to AsyncStorage. On web, SecureStore is unavailable, so AsyncStorage is used.
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

// Android emulators reach the host computer at 10.0.2.2; on a real phone use your PC's LAN IP.
export const DEFAULT_SETTINGS = {
  serverUrl: Platform.OS === 'android' ? 'http://10.0.2.2:8080' : 'http://localhost:8080',
  username: 'student',
  password: 'darija2026',
};

const SETTINGS_KEY = 'settings';
const PASSWORD_KEY = 'translator_password';
const HISTORY_KEY = 'history';
const secure = Platform.OS !== 'web';

export async function loadSettings() {
  const raw = await AsyncStorage.getItem(SETTINGS_KEY);
  const saved = raw ? JSON.parse(raw) : {};
  const password = secure ? await SecureStore.getItemAsync(PASSWORD_KEY) : saved.password;
  return { ...DEFAULT_SETTINGS, ...saved, password: password ?? DEFAULT_SETTINGS.password };
}

export async function saveSettings({ serverUrl, username, password }) {
  if (secure) {
    await SecureStore.setItemAsync(PASSWORD_KEY, password);
    await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify({ serverUrl, username }));
  } else {
    await AsyncStorage.setItem(SETTINGS_KEY, JSON.stringify({ serverUrl, username, password }));
  }
}

export async function loadHistory() {
  const raw = await AsyncStorage.getItem(HISTORY_KEY);
  return raw ? JSON.parse(raw) : [];
}

export async function addToHistory(history, entry, max = 30) {
  const next = [entry, ...history.filter((h) => !(h.text === entry.text && h.script === entry.script))].slice(0, max);
  await AsyncStorage.setItem(HISTORY_KEY, JSON.stringify(next));
  return next;
}

export async function clearHistory() {
  await AsyncStorage.removeItem(HISTORY_KEY);
  return [];
}
