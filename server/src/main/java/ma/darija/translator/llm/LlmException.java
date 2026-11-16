package ma.darija.translator.llm;

/** Raised when the language model cannot produce a translation. */
public class LlmException extends Exception {

    private static final long serialVersionUID = 1L;

    public enum Reason {
        /** The provider rejected our credentials or the model id (server misconfiguration). */
        CONFIGURATION,
        /** Free-tier quota or rate limit reached. */
        RATE_LIMITED,
        /** The model refused or returned nothing usable (e.g. safety block). */
        EMPTY_RESPONSE,
        /** Network failure, timeout or 5xx from the provider. */
        UNAVAILABLE
    }

    private final Reason reason;

    public LlmException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public LlmException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
