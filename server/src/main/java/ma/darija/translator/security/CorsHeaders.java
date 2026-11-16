package ma.darija.translator.security;

import java.util.function.BiConsumer;

/**
 * CORS response headers, shared by the JAX-RS filter and the authentication module
 * (so that even a 401 response can be read by a browser-based client).
 */
public final class CorsHeaders {

    private CorsHeaders() {
    }

    public static void apply(String allowedOrigin, BiConsumer<String, String> setHeader) {
        setHeader.accept("Access-Control-Allow-Origin", allowedOrigin);
        setHeader.accept("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        setHeader.accept("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept");
        setHeader.accept("Access-Control-Max-Age", "3600");
        if (!"*".equals(allowedOrigin)) {
            setHeader.accept("Vary", "Origin");
        }
    }
}
