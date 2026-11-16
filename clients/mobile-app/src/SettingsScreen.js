import { useState } from 'react';
import {
  ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
} from 'react-native';
import { health, translate } from './api';
import { saveSettings } from './storage';
import { useTheme } from './theme';

export default function SettingsScreen({ initial, onClose }) {
  const t = useTheme();
  const s = styles(t);
  const [form, setForm] = useState(initial);
  const [status, setStatus] = useState(null);
  const [busy, setBusy] = useState(false);

  const set = (key) => (value) => setForm((f) => ({ ...f, [key]: value }));

  async function test() {
    setBusy(true);
    setStatus(null);
    try {
      const h = await health(form.serverUrl);
      const r = await translate(form, 'Hello');
      setStatus({ ok: true, text: `Connected (${h.provider} / ${h.model}). "Hello" → ${r.translation}` });
    } catch (e) {
      setStatus({ ok: false, text: e.message });
    } finally {
      setBusy(false);
    }
  }

  async function save() {
    await saveSettings(form);
    onClose(form);
  }

  return (
    <KeyboardAvoidingView style={s.flex} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={s.container} keyboardShouldPersistTaps="handled">
        <Text style={s.title}>Settings</Text>

        <Text style={s.label}>Server URL</Text>
        <TextInput style={s.input} value={form.serverUrl} onChangeText={set('serverUrl')}
          autoCapitalize="none" autoCorrect={false} keyboardType="url" placeholder="http://192.168.1.20:8080"
          placeholderTextColor={t.muted} />
        <Text style={s.hint}>
          Android emulator: http://10.0.2.2:8080 · Real phone: your computer's Wi-Fi IP, e.g. http://192.168.1.20:8080
        </Text>

        <Text style={s.label}>Username</Text>
        <TextInput style={s.input} value={form.username} onChangeText={set('username')}
          autoCapitalize="none" autoCorrect={false} textContentType="username" />

        <Text style={s.label}>Password</Text>
        <TextInput style={s.input} value={form.password} onChangeText={set('password')}
          secureTextEntry autoCapitalize="none" textContentType="password" />
        <Text style={s.hint}>Stored in the device's secure keystore.</Text>

        {status && (
          <Text style={[s.status, { color: status.ok ? t.green : t.red }]}>
            {status.ok ? '✓ ' : '✗ '}{status.text}
          </Text>
        )}

        <View style={s.row}>
          <Pressable style={[s.button, s.ghost]} onPress={test} disabled={busy}>
            {busy ? <ActivityIndicator color={t.text} /> : <Text style={s.ghostText}>Test connection</Text>}
          </Pressable>
          <Pressable style={[s.button, s.primary]} onPress={save}>
            <Text style={s.primaryText}>Save</Text>
          </Pressable>
        </View>
        <Pressable onPress={() => onClose(null)} style={s.cancel}>
          <Text style={{ color: t.muted }}>Cancel</Text>
        </Pressable>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = (t) => StyleSheet.create({
  flex: { flex: 1 },
  container: { padding: 20, gap: 6 },
  title: { fontSize: 24, fontWeight: '700', color: t.text, marginBottom: 12 },
  label: { fontSize: 13, fontWeight: '600', color: t.muted, marginTop: 10, textTransform: 'uppercase' },
  input: {
    borderWidth: 1, borderColor: t.border, borderRadius: 10, padding: 12, fontSize: 16, color: t.text, backgroundColor: t.card,
  },
  hint: { fontSize: 12, color: t.muted },
  status: { marginTop: 14, fontSize: 14 },
  row: { flexDirection: 'row', gap: 10, marginTop: 20 },
  button: { flex: 1, borderRadius: 10, paddingVertical: 13, alignItems: 'center' },
  primary: { backgroundColor: t.red },
  primaryText: { color: t.onAccent, fontWeight: '700', fontSize: 16 },
  ghost: { borderWidth: 1, borderColor: t.border },
  ghostText: { color: t.text, fontSize: 16 },
  cancel: { alignItems: 'center', padding: 14 },
});
