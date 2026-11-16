package ma.darija.translator.config;

import java.util.Locale;
import java.util.function.Function;

/**
 * Immutable application configuration.
 * <p>
 * Every setting is read first from a JVM system property ({@code -Dtranslator.port=9090}),
 * then from an environment variable ({@code PORT=9090}), then falls back to a default.
 * This keeps secrets such as the Gemini API key out of the source code.
 */
public record AppConfig(
        int port,
        String llmProvider,
        String geminiApiKey,
        String geminiModel,
        String geminiBaseUrl,
        String ollamaBaseUrl,
        String ollamaModel,
        String users,
        String authRealm,
        int maxTextLength,
        int llmTimeoutSeconds,
        int cacheSize,
        String corsAllowedOrigin) {

    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_OLLAMA = "ollama";
    public static final String PROVIDER_MOCK = "mock";

    /** Default demo account. Override with TRANSLATOR_USERS in any real deployment. */
    public static final String DEFAULT_USERS = "student:darija2026";

    public static AppConfig load() {
        return load(AppConfig::lookup);
    }

    /** Package-visible for tests: build the config from any key lookup function. */
    static AppConfig load(Function<String, String> source) {
        String apiKey = get(source, "GEMINI_API_KEY", "");
        // If nobody chose a provider, use Gemini when a key is present, otherwise the offline mock.
        String provider = get(source, "LLM_PROVIDER", apiKey.isBlank() ? PROVIDER_MOCK : PROVIDER_GEMINI)
                .toLowerCase(Locale.ROOT);

        return new AppConfig(
                getInt(source, "PORT", 8080),
                provider,
                apiKey,
                // "gemini-flash-latest" is Google's alias for the current Flash model (free tier).
                get(source, "GEMINI_MODEL", "gemini-flash-latest"),
                get(source, "GEMINI_BASE_URL", "https://generativelanguage.googleapis.com"),
                get(source, "OLLAMA_BASE_URL", "http://localhost:11434"),
                get(source, "OLLAMA_MODEL", "qwen2.5:3b"),
                get(source, "TRANSLATOR_USERS", DEFAULT_USERS),
                get(source, "AUTH_REALM", "Darija Translator"),
                getInt(source, "MAX_TEXT_LENGTH", 5000),
                getInt(source, "LLM_TIMEOUT_SECONDS", 60),
                getInt(source, "CACHE_SIZE", 500),
                get(source, "CORS_ALLOWED_ORIGIN", "*"));
    }

    public boolean usesDefaultUsers() {
        return DEFAULT_USERS.equals(users);
    }

    /** ENV_NAME -> system property translator.env.name */
    private static String lookup(String envName) {
        String prop = "translator." + envName.toLowerCase(Locale.ROOT).replace('_', '.');
        String value = System.getProperty(prop);
        return value != null ? value : System.getenv(envName);
    }

    private static String get(Function<String, String> source, String key, String fallback) {
        String value = source.apply(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static int getInt(Function<String, String> source, String key, int fallback) {
        String value = source.apply(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " must be an integer but was '" + value + "'", e);
        }
    }

    /** Safe for logs: never prints the API key or passwords. */
    @Override
    public String toString() {
        return "AppConfig[port=" + port + ", llmProvider=" + llmProvider
                + ", model=" + (PROVIDER_OLLAMA.equals(llmProvider) ? ollamaModel : geminiModel)
                + ", geminiApiKey=" + (geminiApiKey.isBlank() ? "<not set>" : "<set>")
                + ", maxTextLength=" + maxTextLength + ", cacheSize=" + cacheSize + "]";
    }
}
