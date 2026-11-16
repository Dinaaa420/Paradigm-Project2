package ma.darija.translator.security;

import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import ma.darija.translator.config.AppConfig;

import java.util.Set;
import java.util.logging.Logger;

/**
 * Installs {@link BasicAuthServerAuthModule} for this web application when it starts, using the
 * standard Jakarta Authentication 3.0 API {@link AuthConfigFactory#registerServerAuthModule}.
 * <p>
 * Registered programmatically by the embedded server, and also annotated {@link WebListener}
 * so it is picked up automatically if the app is deployed as a WAR to an application server.
 */
@WebListener
public class JakartaAuthenticationInitializer implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(JakartaAuthenticationInitializer.class.getName());

    /** Paths reachable without credentials (relative to the context path). */
    public static final Set<String> PUBLIC_PATHS = Set.of("/api/health");

    private String registrationId;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        AppConfig config = AppConfig.load();
        UserStore users = new UserStore(config.users());
        BasicAuthServerAuthModule module = new BasicAuthServerAuthModule(
                users, config.authRealm(), PUBLIC_PATHS, config.corsAllowedOrigin());

        ServletContext context = event.getServletContext();
        registrationId = AuthConfigFactory.getFactory().registerServerAuthModule(module, context);

        LOG.info("Jakarta Authentication: Basic auth module registered for " + users.size() + " user(s)");
        if (config.usesDefaultUsers()) {
            LOG.warning("Using the default demo account student/darija2026. Set TRANSLATOR_USERS for real use.");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (registrationId != null) {
            AuthConfigFactory.getFactory().removeRegistration(registrationId);
        }
    }
}
