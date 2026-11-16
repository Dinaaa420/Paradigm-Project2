package ma.darija.translator.rest.dto;

/** Uniform JSON error body: {"error":"bad_request","message":"text must not be empty","status":400} */
public record ErrorResponse(String error, String message, int status) {
}
