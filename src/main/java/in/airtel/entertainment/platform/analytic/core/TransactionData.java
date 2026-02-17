package in.airtel.entertainment.platform.analytic.core;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionData {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZ";
    private static final String TIMEZONE = "UTC";

    private final String transactionName;
    private final long startTime;
    private final ConcurrentHashMap<String, Object> data;

    public TransactionData(String transactionName) {
        this.transactionName = transactionName;
        this.startTime = System.currentTimeMillis();
        this.data = new ConcurrentHashMap<>();
    }

    public String getTransactionName() {
        return transactionName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void put(String key, Object value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    public void putAll(Map<String, Object> values) {
        if (values != null) {
            values.forEach((k, v) -> {
                if (k != null && v != null) {
                    data.put(k, v);
                }
            });
        }
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(data);
    }

    public Map<String, Object> toEndMap(Throwable error) {
        long endTime = System.currentTimeMillis();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactionName", transactionName);
        result.put("startTime", formatTime(startTime));
        result.put("endTime", formatTime(endTime));
        result.put("timeTaken", endTime - startTime);
        result.putAll(data);
        if (error != null) {
            result.put("exceptionMessage", error.getMessage());
            result.put("exceptionClass", error.getClass().getName());
        }
        return result;
    }

    private static String formatTime(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(TIMEZONE));
        return sdf.format(new Date(millis));
    }
}
