package ma.darija.translator.llm;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline stand-in for a real model: knows a few phrases and otherwise echoes the input.
 * Used by the automated tests and handy for demos without an API key or internet.
 */
public class MockLlmClient implements LlmClient {

    private static final Pattern TEXT = Pattern.compile("<text>\\n(.*)\\n</text>\\s*$", Pattern.DOTALL);

    private static final Map<String, String[]> PHRASES = Map.of(
            "hello", new String[]{"سلام", "salam"},
            "good morning", new String[]{"صباح الخير", "sba7 lkhir"},
            "thank you", new String[]{"شكرا بزاف", "choukran bzaf"},
            "how are you?", new String[]{"كي داير؟", "kidayr?"},
            "i want coffee", new String[]{"بغيت شي قهوة", "bghit chi 9ahwa"},
            "where is the train station?", new String[]{"فين كاينة لاڭار؟", "fin kayna lagar?"},
            "bonjour", new String[]{"صباح الخير", "sba7 lkhir"},
            "merci", new String[]{"شكرا", "choukran"});

    @Override
    public String generate(String systemInstruction, String userMessage) {
        Matcher m = TEXT.matcher(userMessage);
        String text = m.find() ? m.group(1) : userMessage;
        boolean latin = systemInstruction.contains("Latin script");

        String[] known = PHRASES.get(text.trim().toLowerCase(Locale.ROOT));
        if (known != null) {
            return latin ? known[1] : known[0];
        }
        return "[mock] " + text;
    }

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public String model() {
        return "mock-dictionary";
    }
}
