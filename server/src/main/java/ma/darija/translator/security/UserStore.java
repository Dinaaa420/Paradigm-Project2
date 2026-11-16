package ma.darija.translator.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Identity store for the Basic authentication module.
 * <p>
 * Users come from the TRANSLATOR_USERS setting, a comma-separated list of {@code user:password}.
 * A password may also be given as a SHA-256 hash so it never appears in clear text:
 * {@code alice:{SHA-256}2bb80d53...}  (hash with: {@code printf 'secret' | sha256sum}).
 */
public class UserStore {

    /** Role granted to every authenticated caller. */
    public static final String ROLE_TRANSLATOR = "translator";

    private static final String SHA256_PREFIX = "{SHA-256}";

    private final Map<String, byte[]> passwordHashes = new HashMap<>();

    public UserStore(String usersSpec) {
        if (usersSpec == null || usersSpec.isBlank()) {
            throw new IllegalArgumentException("TRANSLATOR_USERS must define at least one user");
        }
        for (String entry : usersSpec.split(",")) {
            String trimmed = entry.trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon == trimmed.length() - 1) {
                throw new IllegalArgumentException("Invalid user entry (expected user:password): " + trimmed);
            }
            String user = trimmed.substring(0, colon);
            String secret = trimmed.substring(colon + 1);
            byte[] hash = secret.regionMatches(true, 0, SHA256_PREFIX, 0, SHA256_PREFIX.length())
                    ? HexFormat.of().parseHex(secret.substring(SHA256_PREFIX.length()).toLowerCase(Locale.ROOT))
                    : sha256(secret);
            passwordHashes.put(user, hash);
        }
    }

    /**
     * Checks a username/password pair. Comparison is done on hashes with
     * {@link MessageDigest#isEqual}, which runs in constant time (no timing attack).
     *
     * @return the caller's groups when valid, empty otherwise
     */
    public Optional<Set<String>> validate(String username, String password) {
        byte[] expected = passwordHashes.get(username);
        byte[] actual = sha256(password == null ? "" : password);
        if (expected == null) {
            MessageDigest.isEqual(actual, actual); // same work for unknown users
            return Optional.empty();
        }
        return MessageDigest.isEqual(expected, actual) ? Optional.of(Set.of(ROLE_TRANSLATOR)) : Optional.empty();
    }

    public int size() {
        return passwordHashes.size();
    }

    static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
