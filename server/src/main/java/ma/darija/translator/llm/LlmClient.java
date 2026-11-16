package ma.darija.translator.llm;

/**
 * Abstraction over a Large Language Model. The translation service only talks to this
 * interface, so the backing model (Gemini in the cloud, a local Ollama model, or a mock)
 * can be swapped with a single configuration value.
 */
public interface LlmClient {

    /**
     * Sends one prompt to the model and returns its text answer.
     *
     * @param systemInstruction how the model should behave (role, rules, output format)
     * @param userMessage       the actual input for this request
     */
    String generate(String systemInstruction, String userMessage) throws LlmException;

    /** Short provider id, e.g. "gemini". */
    String provider();

    /** Model id, e.g. "gemini-flash-latest". */
    String model();
}
