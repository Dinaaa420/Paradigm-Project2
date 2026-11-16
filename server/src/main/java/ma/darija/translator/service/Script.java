package ma.darija.translator.service;

import java.util.Locale;

/** Writing system for the Darija output. */
public enum Script {
    /** دارجة بالحروف العربية */
    ARABIC,
    /** "Arabizi" / chat alphabet: darija b l7orouf latiniya */
    LATIN;

    public static Script parse(String value) {
        if (value == null || value.isBlank()) {
            return ARABIC;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("script must be 'arabic' or 'latin'");
        }
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
