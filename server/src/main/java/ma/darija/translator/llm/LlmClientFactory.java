package ma.darija.translator.llm;

import ma.darija.translator.config.AppConfig;

/** Picks the {@link LlmClient} implementation from configuration (Factory pattern). */
public final class LlmClientFactory {

    private LlmClientFactory() {
    }

    public static LlmClient create(AppConfig config) {
        return switch (config.llmProvider()) {
            case AppConfig.PROVIDER_GEMINI -> new GeminiClient(
                    config.geminiApiKey(), config.geminiModel(), config.geminiBaseUrl(), config.llmTimeoutSeconds());
            case AppConfig.PROVIDER_OLLAMA -> new OllamaClient(
                    config.ollamaBaseUrl(), config.ollamaModel(), config.llmTimeoutSeconds());
            case AppConfig.PROVIDER_MOCK -> new MockLlmClient();
            default -> throw new IllegalArgumentException(
                    "Unknown LLM_PROVIDER '" + config.llmProvider() + "' (expected gemini, ollama or mock)");
        };
    }
}
