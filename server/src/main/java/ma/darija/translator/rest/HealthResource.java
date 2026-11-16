package ma.darija.translator.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import ma.darija.translator.service.TranslationService;

import java.util.LinkedHashMap;
import java.util.Map;

/** Public, unauthenticated liveness endpoint used by clients to test the server URL. */
@Path("/health")
public class HealthResource {

    private final TranslationService service;

    public HealthResource(TranslationService service) {
        this.service = service;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "darija-translator");
        body.put("provider", service.provider());
        body.put("model", service.model());
        return body;
    }
}
