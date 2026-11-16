package ma.darija.translator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ma.darija.translator.config.AppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end test: boots the real embedded Tomcat + Jakarta REST + Jakarta Authentication stack
 * on a random port, with the offline mock LLM, and talks to it over HTTP.
 */
class TranslatorServerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String GOOD = basic("tester", "s3cret");
    private static TranslatorServer server;
    private static String base;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("translator.port", "0");
        System.setProperty("translator.llm.provider", "mock");
        System.setProperty("translator.translator.users", "tester:s3cret,other:{SHA-256}"
                + "2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b"); // "secret"
        server = new TranslatorServer(AppConfig.load());
        server.start();
        base = "http://localhost:" + server.port() + "/api";
    }

    @AfterAll
    static void stop() throws Exception {
        server.stop();
    }

    @Test
    void healthIsPublic() throws Exception {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/health")).GET());
        assertEquals(200, r.statusCode());
        assertEquals("UP", JSON.readTree(r.body()).path("status").asText());
    }

    @Test
    void translateWithoutCredentialsIsChallenged() throws Exception {
        HttpResponse<String> r = send(postJson("{\"text\":\"Hello\"}", null));
        assertEquals(401, r.statusCode());
        assertTrue(r.headers().firstValue("WWW-Authenticate").orElse("").startsWith("Basic realm="));
        assertEquals("*", r.headers().firstValue("Access-Control-Allow-Origin").orElse(null));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        assertEquals(401, send(postJson("{\"text\":\"Hello\"}", basic("tester", "nope"))).statusCode());
        assertEquals(401, send(postJson("{\"text\":\"Hello\"}", basic("ghost", "s3cret"))).statusCode());
    }

    @Test
    void translateJson() throws Exception {
        HttpResponse<String> r = send(postJson("{\"text\":\"Hello\",\"sourceLanguage\":\"English\"}", GOOD));
        assertEquals(200, r.statusCode(), r.body());
        JsonNode body = JSON.readTree(r.body());
        assertEquals("سلام", body.path("translation").asText());
        assertEquals("ary", body.path("targetLanguage").asText());
        assertEquals("English", body.path("sourceLanguage").asText());
        assertEquals("mock", body.path("provider").asText());
        assertEquals("tester", body.path("user").asText());
    }

    @Test
    void hashedPasswordWorks() throws Exception {
        assertEquals(200, send(postJson("{\"text\":\"Hello\"}", basic("other", "secret"))).statusCode());
    }

    @Test
    void translateLatinScriptViaGet() throws Exception {
        String url = base + "/translate?script=latin&text=" + URLEncoder.encode("Thank you", StandardCharsets.UTF_8);
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", GOOD).GET());
        assertEquals(200, r.statusCode(), r.body());
        assertEquals("choukran bzaf", JSON.readTree(r.body()).path("translation").asText());
    }

    @Test
    void translatePlainText() throws Exception {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/translate"))
                .header("Authorization", GOOD)
                .header("Content-Type", "text/plain; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString("How are you?", StandardCharsets.UTF_8)));
        assertEquals(200, r.statusCode(), r.body());
        assertEquals("كي داير؟", r.body());
    }

    @Test
    void secondIdenticalRequestIsCached() throws Exception {
        send(postJson("{\"text\":\"Good morning\"}", GOOD));
        JsonNode second = JSON.readTree(send(postJson("{\"text\":\"Good morning\"}", GOOD)).body());
        assertTrue(second.path("cached").asBoolean());
    }

    @Test
    void emptyTextIsBadRequest() throws Exception {
        HttpResponse<String> r = send(postJson("{\"text\":\"   \"}", GOOD));
        assertEquals(400, r.statusCode());
        assertEquals("bad_request", JSON.readTree(r.body()).path("error").asText());
    }

    @Test
    void invalidScriptIsBadRequest() throws Exception {
        assertEquals(400, send(postJson("{\"text\":\"Hi\",\"script\":\"klingon\"}", GOOD)).statusCode());
    }

    @Test
    void corsPreflightNeedsNoCredentials() throws Exception {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/translate"))
                .header("Origin", "http://example.com")
                .header("Access-Control-Request-Method", "POST")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()));
        assertTrue(r.statusCode() == 200 || r.statusCode() == 204, "status " + r.statusCode());
        assertNotNull(r.headers().firstValue("Access-Control-Allow-Methods").orElse(null));
    }

    @Test
    void unknownPathIsJson404() throws Exception {
        HttpResponse<String> r = send(HttpRequest.newBuilder(URI.create(base + "/nope")).header("Authorization", GOOD).GET());
        assertEquals(404, r.statusCode());
        assertFalse(r.body().isBlank());
    }

    private static HttpRequest.Builder postJson(String json, String authorization) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + "/translate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        return authorization == null ? b : b.header("Authorization", authorization);
    }

    private static HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }
}
