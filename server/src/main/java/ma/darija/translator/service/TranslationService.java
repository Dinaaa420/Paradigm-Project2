package ma.darija.translator.service;

import ma.darija.translator.llm.LlmClient;
import ma.darija.translator.llm.LlmException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Business logic: validates input, builds the prompt, calls the LLM, cleans the answer and
 * caches results (identical requests do not burn free-tier quota twice).
 */
public class TranslationService {

    private static final Logger LOG = Logger.getLogger(TranslationService.class.getName());

    private final LlmClient llm;
    private final int maxTextLength;
    private final Map<String, TranslationResult> cache;

    public TranslationService(LlmClient llm, int maxTextLength, int cacheSize) {
        this.llm = llm;
        this.maxTextLength = maxTextLength;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, TranslationResult> eldest) {
                return size() > cacheSize;
            }
        });
    }

    public TranslationResult translate(String text, String sourceLanguage, Script script) throws LlmException {
        if (text == null || text.isBlank()) {
            throw new InvalidRequestException("text must not be empty");
        }
        if (text.length() > maxTextLength) {
            throw new InvalidRequestException("text is too long (" + text.length() + " characters, max " + maxTextLength + ")");
        }
        String source = sourceLanguage == null || sourceLanguage.isBlank() ? "auto" : sourceLanguage.trim();
        if (source.length() > 40) {
            throw new InvalidRequestException("sourceLanguage is too long");
        }
        Script target = script == null ? Script.ARABIC : script;
        String normalized = text.strip();

        long start = System.nanoTime();
        String key = source.toLowerCase() + '|' + target + '|' + normalized;
        TranslationResult hit = cache.get(key);
        if (hit != null) {
            return hit.asCached(elapsedMs(start));
        }

        String raw = llm.generate(PromptBuilder.systemInstruction(source, target), PromptBuilder.userMessage(normalized));
        String translation = clean(raw);
        if (translation.isEmpty()) {
            throw new LlmException(LlmException.Reason.EMPTY_RESPONSE, "The model returned an empty translation");
        }

        TranslationResult result = new TranslationResult(
                translation, source, target, llm.provider(), llm.model(), false, elapsedMs(start));
        cache.put(key, result);
        LOG.fine(() -> "Translated " + normalized.length() + " chars in " + result.durationMs() + " ms");
        return result;
    }

    public String provider() {
        return llm.provider();
    }

    public String model() {
        return llm.model();
    }

    /** Removes wrappers that chatty models sometimes add despite instructions. */
    static String clean(String raw) {
        String s = raw == null ? "" : raw.strip();
        // ```...``` code fences
        if (s.startsWith("```") && s.endsWith("```") && s.length() >= 6) {
            s = s.substring(3, s.length() - 3);
            int newline = s.indexOf('\n');
            if (newline >= 0 && newline < 15 && !s.substring(0, newline).contains(" ")) {
                s = s.substring(newline + 1); // drop a language tag like ```text
            }
            s = s.strip();
        }
        // <text>...</text> echoed back
        s = s.replaceFirst("^<text>\\s*", "").replaceFirst("\\s*</text>$", "");
        // surrounding quotes
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("«") && s.endsWith("»")))) {
            s = s.substring(1, s.length() - 1);
        }
        return s.strip();
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
