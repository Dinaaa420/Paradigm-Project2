package ma.darija.translator;

import ma.darija.translator.config.AppConfig;
import ma.darija.translator.rest.TranslatorApplication;
import ma.darija.translator.security.JakartaAuthenticationInitializer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.authenticator.NonLoginAuthenticator;
import org.apache.catalina.startup.Tomcat;

import java.io.IOException;
import java.nio.file.Files;

/**
 * Embedded Apache Tomcat hosting the Jakarta REST application, protected by the
 * Jakarta Authentication module. No separate application server to install.
 */
public class TranslatorServer {

    /** Jakarta REST implementation servlet (Jersey). */
    static final String JAXRS_SERVLET = "org.glassfish.jersey.servlet.ServletContainer";

    private final AppConfig config;
    private Tomcat tomcat;

    public TranslatorServer(AppConfig config) {
        this.config = config;
    }

    public void start() throws LifecycleException, IOException {
        tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("darija-tomcat").toString());
        tomcat.setPort(config.port());
        tomcat.getConnector().setURIEncoding("UTF-8");

        Context context = tomcat.addContext("", null);
        // Load web-app classes (Jersey, our resources) with the same class loader as this class.
        context.setParentClassLoader(TranslatorServer.class.getClassLoader());
        context.setRequestCharacterEncoding("UTF-8");
        context.setResponseCharacterEncoding("UTF-8");

        // 1) Jakarta Authentication: register our ServerAuthModule when the app starts.
        context.addServletContainerInitializer(
                (classes, servletContext) -> servletContext.addListener(new JakartaAuthenticationInitializer()), null);
        // Tomcat consults registered Jakarta Authentication modules from its authenticator valve.
        context.getPipeline().addValve(new NonLoginAuthenticator());

        // 2) Jakarta REST: the implementation servlet serves TranslatorApplication under /api/*.
        Wrapper jaxrs = Tomcat.addServlet(context, "jakarta-rest", JAXRS_SERVLET);
        jaxrs.addInitParameter("jakarta.ws.rs.Application", TranslatorApplication.class.getName());
        jaxrs.setLoadOnStartup(1);
        context.addServletMappingDecoded("/api/*", "jakarta-rest");

        tomcat.start();
    }

    /** Actual port (useful when configured with port 0 in tests). */
    public int port() {
        return tomcat.getConnector().getLocalPort();
    }

    public void await() {
        tomcat.getServer().await();
    }

    public void stop() throws LifecycleException {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }
}
