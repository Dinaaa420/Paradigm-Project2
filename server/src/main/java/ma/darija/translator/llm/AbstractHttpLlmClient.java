package ma.darija.translator.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Shared JSON-over-HTTP plumbing for remote LLM providers (uses java.net.http). */
abstract class AbstractHttpLlmClient implements LlmClient {

    protected static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient http;
    private final Duration timeout;

    protected AbstractHttpLlmClient(int timeoutSeconds) {
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    protected JsonNode postJson(String url, Object body, Map<String, String> headers) throws LlmException {
        HttpResponse<String> response;
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(timeout)
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body), StandardCharsets.UTF_8));
            headers.forEach(request::header);
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException(LlmException.Reason.UNAVAILABLE, "Interrupted while calling " + provider(), e);
        } catch (IOException e) {
            throw new LlmException(LlmException.Reason.UNAVAILABLE,
                    "Cannot reach " + provider() + " (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")", e);
        }

        int status = response.statusCode();
        if (status == 429) {
            throw new LlmException(LlmException.Reason.RATE_LIMITED, provider() + " rate limit or quota exceeded");
        }
        if (status == 400 || status == 401 || status == 403 || status == 404) {
            throw new LlmException(LlmException.Reason.CONFIGURATION,
                    provider() + " rejected the request (HTTP " + status + "): " + abbreviate(response.body()));
        }
        if (status >= 300) {
            throw new LlmException(LlmException.Reason.UNAVAILABLE,
                    provider() + " returned HTTP " + status + ": " + abbreviate(response.body()));
        }
        try {
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new LlmException(LlmException.Reason.UNAVAILABLE, provider() + " returned invalid JSON", e);
        }
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        String oneLine = s.replaceAll("\\s+", " ").trim();
        return oneLine.length() > 300 ? oneLine.substring(0, 300) + "..." : oneLine;
    }
}
