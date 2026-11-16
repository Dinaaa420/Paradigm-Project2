package ma.darija.translator.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ma.darija.translator.service.TranslationResult;

import java.time.Instant;

/** JSON body returned by /api/translate. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TranslationResponse(
        String translation,
        String sourceLanguage,
        String targetLanguage,
        String script,
        String provider,
        String model,
        boolean cached,
        long durationMs,
        String user,
        String timestamp) {

    /** ISO 639-3 code for Moroccan Arabic. */
    public static final String DARIJA = "ary";

    public static TranslationResponse from(TranslationResult r, String user) {
        return new TranslationResponse(r.translation(), r.sourceLanguage(), DARIJA, r.script().id(),
                r.provider(), r.model(), r.cached(), r.durationMs(), user, Instant.now().toString());
    }
}
