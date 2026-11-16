package ma.darija.translator.service;

import ma.darija.translator.llm.LlmClient;
import ma.darija.translator.llm.LlmException;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslationServiceTest {

    /** Fake LLM that records calls and returns a fixed answer. */
    private static final class FakeLlm implements LlmClient {
        final AtomicInteger calls = new AtomicInteger();
        String answer = "سلام";
        String lastSystem;

        @Override
        public String generate(String systemInstruction, String userMessage) {
            calls.incrementAndGet();
            lastSystem = systemInstruction;
            return answer;
        }

        @Override
        public String provider() {
            return "fake";
        }

        @Override
        public String model() {
            return "fake-1";
        }
    }

    @Test
    void translatesAndCaches() throws LlmException {
        FakeLlm llm = new FakeLlm();
        TranslationService service = new TranslationService(llm, 100, 10);

        TranslationResult first = service.translate("Hello", "English", Script.ARABIC);
        TranslationResult second = service.translate("  Hello ", "english", Script.ARABIC);

        assertEquals("سلام", first.translation());
        assertFalse(first.cached());
        assertTrue(second.cached());
        assertEquals(1, llm.calls.get());
    }

    @Test
    void scriptChangesPrompt() throws LlmException {
        FakeLlm llm = new FakeLlm();
        TranslationService service = new TranslationService(llm, 100, 10);
        service.translate("Hello", "auto", Script.LATIN);
        assertTrue(llm.lastSystem.contains("Latin script"));
        assertTrue(llm.lastSystem.contains("detect it"));
    }

    @Test
    void validatesInput() {
        TranslationService service = new TranslationService(new FakeLlm(), 5, 10);
        assertThrows(InvalidRequestException.class, () -> service.translate("", "auto", Script.ARABIC));
        assertThrows(InvalidRequestException.class, () -> service.translate(null, "auto", Script.ARABIC));
        assertThrows(InvalidRequestException.class, () -> service.translate("too long", "auto", Script.ARABIC));
        assertThrows(InvalidRequestException.class, () -> Script.parse("klingon"));
    }

    @Test
    void emptyModelAnswerIsAnError() {
        FakeLlm llm = new FakeLlm();
        llm.answer = "  ";
        TranslationService service = new TranslationService(llm, 100, 10);
        LlmException e = assertThrows(LlmException.class, () -> service.translate("Hi", "auto", Script.ARABIC));
        assertEquals(LlmException.Reason.EMPTY_RESPONSE, e.reason());
    }

    @Test
    void cleansChattyOutput() {
        assertEquals("سلام", TranslationService.clean("```\nسلام\n```"));
        assertEquals("سلام", TranslationService.clean("```text\nسلام\n```"));
        assertEquals("سلام", TranslationService.clean("\"سلام\""));
        assertEquals("سلام", TranslationService.clean("<text>\nسلام\n</text>"));
        assertEquals("line1\nline2", TranslationService.clean("  line1\nline2  "));
    }
}
