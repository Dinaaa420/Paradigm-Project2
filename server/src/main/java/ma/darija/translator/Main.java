package ma.darija.translator;

import ma.darija.translator.config.AppConfig;

import java.util.logging.Logger;

/** Entry point: {@code java -jar target/darija-translator.jar} */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.load();
        TranslatorServer server = new TranslatorServer(config);
        server.start();

        String base = "http://localhost:" + server.port() + "/api";
        LOG.info("""

                ==========================================================
                 Darija Translator is running (LLM: %s / %s)
                   POST %s/translate   (HTTP Basic auth)
                   GET  %s/health      (public)
                ==========================================================""".formatted(
                config.llmProvider(),
                AppConfig.PROVIDER_OLLAMA.equals(config.llmProvider()) ? config.ollamaModel()
                        : AppConfig.PROVIDER_GEMINI.equals(config.llmProvider()) ? config.geminiModel() : "offline",
                base, base));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (Exception ignored) {
                // shutting down anyway
            }
        }));
        server.await();
    }
}
