package ma.darija.translator.llm;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * A lightweight LLM running locally with Ollama (https://ollama.com), e.g.
 * {@code ollama pull qwen2.5:3b}. No API key, no internet, no quota.
 */
public class OllamaClient extends AbstractHttpLlmClient {

    private final String baseUrl;
    private final String model;

    public OllamaClient(String baseUrl, String model, int timeoutSeconds) {
        super(timeoutSeconds);
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
    }

    @Override
    public String generate(String systemInstruction, String userMessage) throws LlmException {
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "options", Map.of("temperature", 0.2),
                "messages", List.of(
                        Map.of("role", "system", "content", systemInstruction),
                        Map.of("role", "user", "content", userMessage)));

        JsonNode json = postJson(baseUrl + "/api/chat", body, Map.of());
        String text = json.path("message").path("content").asText("");
        if (text.isBlank()) {
            throw new LlmException(LlmException.Reason.EMPTY_RESPONSE, "Ollama returned no text");
        }
        return text;
    }

    @Override
    public String provider() {
        return "ollama";
    }

    @Override
    public String model() {
        return model;
    }
}
