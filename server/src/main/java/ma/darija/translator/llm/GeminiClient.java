package ma.darija.translator.llm;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini via the REST "generateContent" endpoint.
 * Get a free API key at https://aistudio.google.com/apikey
 */
public class GeminiClient extends AbstractHttpLlmClient {

    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiClient(String apiKey, String model, String baseUrl, int timeoutSeconds) {
        super(timeoutSeconds);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("GEMINI_API_KEY is required when LLM_PROVIDER=gemini");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Override
    public String generate(String systemInstruction, String userMessage) throws LlmException {
        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userMessage)))),
                // Low temperature = faithful, repeatable translations.
                "generationConfig", Map.of("temperature", 0.2));

        String url = baseUrl + "/v1beta/models/" + model + ":generateContent";
        // The key goes in a header, not the URL, so it never ends up in access logs.
        JsonNode json = postJson(url, body, Map.of("x-goog-api-key", apiKey));

        String blockReason = json.path("promptFeedback").path("blockReason").asText("");
        if (!blockReason.isEmpty()) {
            throw new LlmException(LlmException.Reason.EMPTY_RESPONSE, "Gemini blocked the prompt: " + blockReason);
        }

        StringBuilder text = new StringBuilder();
        for (JsonNode part : json.path("candidates").path(0).path("content").path("parts")) {
            // Skip "thought" parts that thinking models may return.
            if (!part.path("thought").asBoolean(false)) {
                text.append(part.path("text").asText(""));
            }
        }
        if (text.toString().isBlank()) {
            String finish = json.path("candidates").path(0).path("finishReason").asText("unknown");
            throw new LlmException(LlmException.Reason.EMPTY_RESPONSE, "Gemini returned no text (finishReason=" + finish + ")");
        }
        return text.toString();
    }

    @Override
    public String provider() {
        return "gemini";
    }

    @Override
    public String model() {
        return model;
    }
}
