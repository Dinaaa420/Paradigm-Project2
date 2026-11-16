package ma.darija.translator.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUnitTest {

    @Test
    void parsesBasicHeader() {
        String header = "Basic " + Base64.getEncoder().encodeToString("ali:pa:ss".getBytes(StandardCharsets.UTF_8));
        BasicCredentials c = BasicCredentials.parse(header).orElseThrow();
        assertEquals("ali", c.username());
        assertEquals("pa:ss", c.password()); // password may contain ':'
    }

    @Test
    void rejectsMalformedHeaders() {
        assertTrue(BasicCredentials.parse(null).isEmpty());
        assertTrue(BasicCredentials.parse("Bearer abc").isEmpty());
        assertTrue(BasicCredentials.parse("Basic !!!notbase64").isEmpty());
        assertTrue(BasicCredentials.parse("Basic " + Base64.getEncoder().encodeToString("nocolon".getBytes())).isEmpty());
    }

    @Test
    void userStoreValidatesPlainAndHashedPasswords() {
        UserStore store = new UserStore("a:one, b:{SHA-256}2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b");
        assertEquals(2, store.size());
        assertTrue(store.validate("a", "one").isPresent());
        assertTrue(store.validate("b", "secret").isPresent());
        assertTrue(store.validate("a", "two").isEmpty());
        assertTrue(store.validate("nobody", "one").isEmpty());
        assertTrue(store.validate("a", "one").get().contains(UserStore.ROLE_TRANSLATOR));
    }

    @Test
    void userStoreRejectsBadSpec() {
        assertThrows(IllegalArgumentException.class, () -> new UserStore(""));
        assertThrows(IllegalArgumentException.class, () -> new UserStore("justname"));
    }
}
