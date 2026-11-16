package ma.darija.translator.service;

/** The client sent something we cannot translate (empty text, too long, bad option). Maps to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidRequestException(String message) {
        super(message);
    }
}
