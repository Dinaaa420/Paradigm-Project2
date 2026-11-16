package ma.darija.translator.rest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import ma.darija.translator.llm.LlmException;
import ma.darija.translator.rest.dto.TranslationRequest;
import ma.darija.translator.rest.dto.TranslationResponse;
import ma.darija.translator.service.Script;
import ma.darija.translator.service.TranslationResult;
import ma.darija.translator.service.TranslationService;

import java.security.Principal;

/**
 * The TranslatorResource RESTful endpoint: {@code /api/translate}.
 * <p>
 * Every {@code translate(...)} method takes a text and returns its Moroccan Darija translation.
 * Callers must be authenticated (HTTP Basic, enforced by the Jakarta Authentication module
 * before the request reaches this class).
 *
 * <pre>
 * POST /api/translate   Content-Type: application/json   {"text":"Hello","sourceLanguage":"English","script":"arabic"}
 * POST /api/translate   Content-Type: text/plain         Hello            -> سلام   (plain text back)
 * GET  /api/translate?text=Hello&amp;script=latin
 * </pre>
 */
@Path("/translate")
public class TranslatorResource {

    private final TranslationService service;

    @Context
    private SecurityContext security;

    public TranslatorResource(TranslationService service) {
        this.service = service;
    }

    /** Main business method: JSON in, JSON out. */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translate(TranslationRequest request) throws LlmException {
        if (request == null) {
            request = new TranslationRequest(null, null, null);
        }
        TranslationResult result = service.translate(request.text(), request.sourceLanguage(), Script.parse(request.script()));
        return TranslationResponse.from(result, caller());
    }

    /** Same business method for quick tests from a browser address bar or cURL. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TranslationResponse translate(@QueryParam("text") String text,
                                         @QueryParam("sourceLanguage") @DefaultValue("auto") String sourceLanguage,
                                         @QueryParam("script") @DefaultValue("arabic") String script) throws LlmException {
        return translate(new TranslationRequest(text, sourceLanguage, script));
    }

    /** Simplest possible form: plain text in, plain Darija text out. */
    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN + ";charset=UTF-8")
    public String translate(String text) throws LlmException {
        return service.translate(text, "auto", Script.ARABIC).translation();
    }

    private String caller() {
        Principal principal = security == null ? null : security.getUserPrincipal();
        return principal == null ? null : principal.getName();
    }
}
