package in.airtel.entertainment.platform.analytic.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionData {

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
        result.put("txnName", transactionName);
        result.put("timeTaken", endTime - startTime);
        result.putAll(data);
        if (error != null) {
            result.put("exceptionMessage", error.getMessage());
            result.put("exceptionClass", error.getClass().getName());
        }
        return result;
    }
}
