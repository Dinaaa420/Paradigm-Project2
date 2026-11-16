package ma.darija.translator.rest;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import ma.darija.translator.security.CorsHeaders;

/**
 * Cross-Origin Resource Sharing, so browser-based clients (web pages, Expo web) can call the API.
 * Answers pre-flight OPTIONS requests directly and decorates every response.
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final String allowedOrigin;

    public CorsFilter(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equals(request.getMethod())) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        CorsHeaders.apply(allowedOrigin, (name, value) -> response.getHeaders().putSingle(name, value));
    }
}
