package ma.darija.translator.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * JSON body of POST /api/translate.
 *
 * @param text           text to translate (required)
 * @param sourceLanguage e.g. "English", "French"; "auto" (default) lets the model detect it
 * @param script         "arabic" (default) or "latin" (Arabizi)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TranslationRequest(String text, String sourceLanguage, String script) {
}
