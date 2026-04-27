package in.airtel.entertainment.platform.analytic.encoder;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticJsonEncoderTest {

    @Test
    void shouldFlattenTransactionPayloadAtRoot() {
        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(new LoggerContext());
        event.setTimeStamp(1777280400000L);
        event.setLevel(Level.INFO);
        event.setLoggerName("analyticLogger");
        event.setMessage("{\"txnName\":\"resolveCollection\",\"timeTaken\":12,\"appKey\":\"IPTV\"}");
        event.setMDCPropertyMap(Map.of());

        String encoded = new String(new AnalyticJsonEncoder().encode(event), StandardCharsets.UTF_8);

        assertTrue(encoded.contains("\"txnName\":\"resolveCollection\""));
        assertTrue(encoded.contains("\"timeTaken\":12"));
        assertTrue(encoded.contains("\"appKey\":\"IPTV\""));
        assertFalse(encoded.contains("\"transaction\""));
    }
}
