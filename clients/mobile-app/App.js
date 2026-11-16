import { useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import TranslateScreen from './src/TranslateScreen';
import SettingsScreen from './src/SettingsScreen';
import { loadSettings } from './src/storage';
import { useTheme } from './src/theme';

export default function App() {
  const t = useTheme();
  const [settings, setSettings] = useState(null);
  const [showSettings, setShowSettings] = useState(false);

  useEffect(() => { loadSettings().then(setSettings); }, []);

  return (
    <SafeAreaProvider>
      <SafeAreaView style={{ flex: 1, backgroundColor: t.bg }}>
        <StatusBar style="auto" />
        {!settings ? (
          <View style={{ flex: 1, justifyContent: 'center' }}><ActivityIndicator /></View>
        ) : showSettings ? (
          <SettingsScreen
            initial={settings}
            onClose={(saved) => { if (saved) setSettings(saved); setShowSettings(false); }}
          />
        ) : (
          <TranslateScreen settings={settings} onOpenSettings={() => setShowSettings(true)} />
        )}
      </SafeAreaView>
    </SafeAreaProvider>
  );
}
