package ma.darija.translator.rest;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import ma.darija.translator.config.AppConfig;
import ma.darija.translator.llm.LlmClient;
import ma.darija.translator.llm.LlmClientFactory;
import ma.darija.translator.service.TranslationService;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Jakarta REST application: wires configuration, the LLM client and the service, then exposes
 * the resources under {@code /api}. Only standard Jakarta REST API is used, so any implementation
 * (Jersey here) can run it.
 */
@ApplicationPath("/api")
public class TranslatorApplication extends Application {

    private static final Logger LOG = Logger.getLogger(TranslatorApplication.class.getName());

    private final Set<Object> singletons;

    public TranslatorApplication() {
        AppConfig config = AppConfig.load();
        LlmClient llm = LlmClientFactory.create(config);
        TranslationService service = new TranslationService(llm, config.maxTextLength(), config.cacheSize());
        LOG.info("Translator ready: " + config);

        singletons = Set.of(
                new TranslatorResource(service),
                new HealthResource(service),
                new CorsFilter(config.corsAllowedOrigin()),
                new JacksonJsonProvider());
    }

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                ExceptionMappers.InvalidRequestMapper.class,
                ExceptionMappers.LlmExceptionMapper.class,
                ExceptionMappers.WebApplicationExceptionMapper.class,
                ExceptionMappers.UnexpectedExceptionMapper.class);
    }

    @Override
    @SuppressWarnings("deprecation") // fine for a small app without CDI
    public Set<Object> getSingletons() {
        return singletons;
    }
}
