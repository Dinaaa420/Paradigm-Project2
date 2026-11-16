package ma.darija.translator.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ma.darija.translator.llm.LlmException;
import ma.darija.translator.rest.dto.ErrorResponse;
import ma.darija.translator.service.InvalidRequestException;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Turns exceptions into consistent JSON error responses. */
public final class ExceptionMappers {

    private static final Logger LOG = Logger.getLogger(ExceptionMappers.class.getName());

    private ExceptionMappers() {
    }

    static Response json(Response.Status status, String error, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(error, message, status.getStatusCode()))
                .build();
    }

    /** 400 - bad input. */
    @Provider
    public static class InvalidRequestMapper implements ExceptionMapper<InvalidRequestException> {
        @Override
        public Response toResponse(InvalidRequestException e) {
            return json(Response.Status.BAD_REQUEST, "bad_request", e.getMessage());
        }
    }

    /** 429 / 502 / 503 - the language model failed. */
    @Provider
    public static class LlmExceptionMapper implements ExceptionMapper<LlmException> {
        @Override
        public Response toResponse(LlmException e) {
            LOG.warning("LLM failure (" + e.reason() + "): " + e.getMessage());
            return switch (e.reason()) {
                case RATE_LIMITED -> json(Response.Status.TOO_MANY_REQUESTS, "rate_limited",
                        "The translation model quota is exhausted, please retry in a minute.");
                case CONFIGURATION -> json(Response.Status.BAD_GATEWAY, "llm_configuration",
                        "The translation model rejected the request. Check the server's LLM settings.");
                case EMPTY_RESPONSE -> json(Response.Status.BAD_GATEWAY, "llm_empty_response",
                        "The translation model returned no translation for this text.");
                case UNAVAILABLE -> json(Response.Status.SERVICE_UNAVAILABLE, "llm_unavailable",
                        "The translation model is unreachable right now.");
            };
        }
    }

    /** Keeps 404/405/415... from the framework, but with a JSON body. */
    @Provider
    public static class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {
        @Override
        public Response toResponse(WebApplicationException e) {
            Response.Status status = Response.Status.fromStatusCode(e.getResponse().getStatus());
            if (status == null) {
                return e.getResponse();
            }
            String error = status.getReasonPhrase().toLowerCase().replace(' ', '_');
            return json(status, error, e.getMessage());
        }
    }

    /** 500 - anything unexpected; details stay in the server log. */
    @Provider
    public static class UnexpectedExceptionMapper implements ExceptionMapper<RuntimeException> {
        @Override
        public Response toResponse(RuntimeException e) {
            if (e instanceof WebApplicationException wae) {
                return new WebApplicationExceptionMapper().toResponse(wae);
            }
            LOG.log(Level.SEVERE, "Unexpected error", e);
            return json(Response.Status.INTERNAL_SERVER_ERROR, "internal_error", "Unexpected server error");
        }
    }
}
