package in.airtel.entertainment.platform.analytic.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class AnalyticJsonEncoder extends EncoderBase<ILoggingEvent> {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private static final byte[] LINE_SEP = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{");

        // @timestamp
        Instant instant = Instant.ofEpochMilli(event.getTimeStamp());
        sb.append("\"@timestamp\":\"").append(TIMESTAMP_FORMAT.format(instant)).append("\"");

        // level
        sb.append(",\"level\":\"").append(event.getLevel().toString()).append("\"");
        sb.append(",\"log_type\":\"LOGSTASH\"");

        // loggerName
        sb.append(",\"loggerName\":\"").append(escapeJson(event.getLoggerName())).append("\"");

        // MDC: include all MDC properties (e.g., correlationid)
        if (event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty()) {
            event.getMDCPropertyMap().forEach((key, value) -> {
                sb.append(",\"").append(escapeJson(key)).append("\":\"")
                  .append(escapeJson(value)).append("\"");
            });
        }

        // The message itself is the transaction JSON — embed as "transaction" field
        String message = event.getFormattedMessage();
        if (message != null && message.startsWith("{") && message.endsWith("}")) {
            sb.append(",\"transaction\":").append(message);
        } else if (message != null) {
            sb.append(",\"message\":\"").append(escapeJson(message)).append("\"");
        }

        sb.append("}");

        byte[] jsonBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[jsonBytes.length + LINE_SEP.length];
        System.arraycopy(jsonBytes, 0, result, 0, jsonBytes.length);
        System.arraycopy(LINE_SEP, 0, result, jsonBytes.length, LINE_SEP.length);
        return result;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
