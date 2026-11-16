import { useEffect, useState } from 'react';
import {
  ActivityIndicator, FlatList, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View,
} from 'react-native';
import * as Clipboard from 'expo-clipboard';
import { translate } from './api';
import { speak, stop } from './speech';
import { addToHistory, clearHistory, loadHistory } from './storage';
import { useTheme } from './theme';

const LANGUAGES = [
  { id: 'auto', label: 'Auto' }, { id: 'English', label: 'English' }, { id: 'French', label: 'Français' },
  { id: 'Spanish', label: 'Español' }, { id: 'Modern Standard Arabic', label: 'الفصحى' },
];

export default function TranslateScreen({ settings, onOpenSettings }) {
  const t = useTheme();
  const s = styles(t);
  const [text, setText] = useState('');
  const [source, setSource] = useState('auto');
  const [script, setScript] = useState('arabic');
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [speaking, setSpeaking] = useState(false);
  const [history, setHistory] = useState([]);

  useEffect(() => { loadHistory().then(setHistory); }, []);

  async function run(input = text, targetScript = script) {
    const value = input.trim();
    if (!value) return;
    setLoading(true);
    setError(null);
    try {
      const r = await translate(settings, value, { sourceLanguage: source, script: targetScript });
      setResult(r);
      setHistory(await addToHistory(history, { text: value, translation: r.translation, script: r.script, at: Date.now() }));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  function changeScript(next) {
    setScript(next);
    if (result && text.trim()) run(text, next);
  }

  async function pasteFromClipboard() {
    const clip = await Clipboard.getStringAsync();
    if (clip) {
      setText(clip);
      run(clip);
    }
  }

  async function readAloud() {
    if (speaking) {
      await stop();
      setSpeaking(false);
      return;
    }
    setSpeaking(true);
    await speak(result.translation, result.script, { onDone: () => setSpeaking(false) });
  }

  const header = (
    <View style={s.inner}>
      <View style={s.topBar}>
        <Text style={s.title}>Darija Translator</Text>
        <Pressable onPress={onOpenSettings} hitSlop={12} accessibilityLabel="Settings">
          <Text style={s.gear}>⚙︎</Text>
        </Pressable>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.chips}>
        {LANGUAGES.map((l) => (
          <Pressable key={l.id} onPress={() => setSource(l.id)} style={[s.chip, source === l.id && s.chipOn]}>
            <Text style={[s.chipText, source === l.id && s.chipTextOn]}>{l.label}</Text>
          </Pressable>
        ))}
      </ScrollView>

      <View style={s.card}>
        <TextInput
          style={s.input} multiline value={text} onChangeText={setText} maxLength={5000}
          placeholder="Type or paste text…" placeholderTextColor={t.muted} textAlignVertical="top"
        />
        <View style={s.row}>
          <Pressable onPress={pasteFromClipboard} style={s.small}><Text style={s.smallText}>📋 Paste & translate</Text></Pressable>
          {text.length > 0 && (
            <Pressable onPress={() => { setText(''); setResult(null); }} style={s.small}><Text style={s.smallText}>Clear</Text></Pressable>
          )}
        </View>
      </View>

      <View style={s.row}>
        <View style={s.segment}>
          {[['arabic', 'العربية'], ['latin', 'Latin 3·7·9']].map(([id, label]) => (
            <Pressable key={id} onPress={() => changeScript(id)} style={[s.segItem, script === id && s.segOn]}>
              <Text style={[s.segText, script === id && s.segTextOn]}>{label}</Text>
            </Pressable>
          ))}
        </View>
        <Pressable style={[s.translate, loading && { opacity: 0.6 }]} onPress={() => run()} disabled={loading}>
          {loading ? <ActivityIndicator color={t.onAccent} /> : <Text style={s.translateText}>Translate</Text>}
        </Pressable>
      </View>

      {error && <Text style={s.error}>{error}</Text>}

      {result && (
        <View style={s.card}>
          <Text selectable style={[s.output, { writingDirection: result.script === 'arabic' ? 'rtl' : 'ltr', textAlign: result.script === 'arabic' ? 'right' : 'left' }]}>
            {result.translation}
          </Text>
          <View style={s.row}>
            <Pressable onPress={readAloud} style={s.small}><Text style={s.smallText}>{speaking ? '⏹ Stop' : '🔊 Read aloud'}</Text></Pressable>
            <Pressable onPress={() => Clipboard.setStringAsync(result.translation)} style={s.small}><Text style={s.smallText}>Copy</Text></Pressable>
            <Text style={s.meta}>{result.provider} · {result.cached ? 'cached' : `${result.durationMs} ms`}</Text>
          </View>
        </View>
      )}

      {history.length > 0 && (
        <View style={[s.row, { marginTop: 8 }]}>
          <Text style={s.section}>History</Text>
          <Pressable style={{ marginLeft: 'auto' }} onPress={async () => setHistory(await clearHistory())}>
            <Text style={s.meta}>Clear</Text>
          </Pressable>
        </View>
      )}
    </View>
  );

  return (
    <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <FlatList
        data={history}
        keyExtractor={(h) => `${h.at}`}
        ListHeaderComponent={header}
        keyboardShouldPersistTaps="handled"
        contentContainerStyle={{ paddingBottom: 32 }}
        renderItem={({ item }) => (
          <Pressable
            style={s.historyItem}
            onPress={() => { setText(item.text); setResult({ ...item, provider: 'history', cached: true }); }}
          >
            <Text numberOfLines={1} style={s.historySrc}>{item.text}</Text>
            <Text style={[s.historyDst, { textAlign: item.script === 'arabic' ? 'right' : 'left' }]}>{item.translation}</Text>
          </Pressable>
        )}
      />
    </KeyboardAvoidingView>
  );
}

const styles = (t) => StyleSheet.create({
  inner: { padding: 16, gap: 12 },
  topBar: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  title: { fontSize: 24, fontWeight: '800', color: t.red },
  gear: { fontSize: 26, color: t.muted },
  chips: { gap: 8 },
  chip: { paddingHorizontal: 14, paddingVertical: 7, borderRadius: 20, borderWidth: 1, borderColor: t.border, backgroundColor: t.card },
  chipOn: { backgroundColor: t.green, borderColor: t.green },
  chipText: { color: t.text, fontSize: 14 },
  chipTextOn: { color: t.onAccent, fontWeight: '600' },
  card: { backgroundColor: t.card, borderRadius: 14, borderWidth: 1, borderColor: t.border, padding: 12, gap: 10 },
  input: { minHeight: 110, fontSize: 17, color: t.text },
  row: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  small: { paddingVertical: 6, paddingHorizontal: 10, borderRadius: 8, borderWidth: 1, borderColor: t.border },
  smallText: { color: t.text, fontSize: 14 },
  segment: { flex: 1, flexDirection: 'row', borderRadius: 10, borderWidth: 1, borderColor: t.border, overflow: 'hidden', backgroundColor: t.card },
  segItem: { flex: 1, paddingVertical: 11, alignItems: 'center' },
  segOn: { backgroundColor: t.green },
  segText: { color: t.text, fontSize: 14 },
  segTextOn: { color: t.onAccent, fontWeight: '600' },
  translate: { backgroundColor: t.red, borderRadius: 10, paddingVertical: 12, paddingHorizontal: 20, minWidth: 110, alignItems: 'center' },
  translateText: { color: t.onAccent, fontWeight: '700', fontSize: 16 },
  error: { color: t.red, backgroundColor: t.errorBg, padding: 10, borderRadius: 10, overflow: 'hidden' },
  output: { fontSize: 24, lineHeight: 38, color: t.text },
  meta: { color: t.muted, fontSize: 12, marginLeft: 'auto' },
  section: { color: t.muted, fontWeight: '700', textTransform: 'uppercase', fontSize: 13 },
  historyItem: { marginHorizontal: 16, marginBottom: 8, padding: 12, borderRadius: 12, backgroundColor: t.card, borderWidth: 1, borderColor: t.border },
  historySrc: { color: t.muted, fontSize: 13 },
  historyDst: { color: t.text, fontSize: 18, marginTop: 2 },
});
