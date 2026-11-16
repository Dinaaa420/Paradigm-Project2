package ma.darija.translator.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/** Parses an HTTP {@code Authorization: Basic base64(user:password)} header (RFC 7617). */
public record BasicCredentials(String username, String password) {

    public static Optional<BasicCredentials> parse(String authorizationHeader) {
        if (authorizationHeader == null) {
            return Optional.empty();
        }
        String header = authorizationHeader.trim();
        if (header.length() < 6 || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return Optional.empty();
        }
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        int colon = decoded.indexOf(':');
        if (colon <= 0) {
            return Optional.empty();
        }
        return Optional.of(new BasicCredentials(decoded.substring(0, colon), decoded.substring(colon + 1)));
    }

    @Override
    public String toString() {
        return "BasicCredentials[username=" + username + ", password=***]";
    }
}
