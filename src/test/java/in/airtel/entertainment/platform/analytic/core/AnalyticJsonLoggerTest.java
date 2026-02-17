package in.airtel.entertainment.platform.analytic.core;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticJsonLoggerTest {

    @Test
    void shouldSerializeSimpleMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionName", "testTx");
        data.put("timeTaken", 245);
        data.put("success", true);

        String json = AnalyticJsonLogger.toJson(data);

        assertEquals("{\"transactionName\":\"testTx\",\"timeTaken\":245,\"success\":true}", json);
    }

    @Test
    void shouldHandleNullValues() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", null);

        String json = AnalyticJsonLogger.toJson(data);

        assertEquals("{\"key\":null}", json);
    }

    @Test
    void shouldEscapeSpecialCharacters() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "line1\nline2\ttab\"quote");

        String json = AnalyticJsonLogger.toJson(data);

        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\t"));
        assertTrue(json.contains("\\\"quote"));
    }
}
