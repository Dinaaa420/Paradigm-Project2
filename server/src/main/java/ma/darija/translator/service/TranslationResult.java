package ma.darija.translator.service;

/** Outcome of one translation, independent of how it is exposed (REST, CLI, ...). */
public record TranslationResult(
        String translation,
        String sourceLanguage,
        Script script,
        String provider,
        String model,
        boolean cached,
        long durationMs) {

    TranslationResult asCached(long durationMs) {
        return new TranslationResult(translation, sourceLanguage, script, provider, model, true, durationMs);
    }
}
