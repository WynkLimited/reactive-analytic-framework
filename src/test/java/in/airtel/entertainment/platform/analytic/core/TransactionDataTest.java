package in.airtel.entertainment.platform.analytic.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TransactionDataTest {

    @Test
    void shouldStoreAndRetrieveValues() {
        TransactionData td = new TransactionData("testTx");
        td.put("key1", "value1");
        td.put("key2", 42);

        assertEquals("value1", td.get("key1"));
        assertEquals(42, td.get("key2"));
        assertEquals("testTx", td.getTransactionName());
    }

    @Test
    void shouldIgnoreNullKeysAndValues() {
        TransactionData td = new TransactionData("testTx");
        td.put(null, "value");
        td.put("key", null);

        assertTrue(td.getData().isEmpty());
    }

    @Test
    void putAllShouldMergeValues() {
        TransactionData td = new TransactionData("testTx");
        td.putAll(Map.of("a", 1, "b", 2));

        assertEquals(1, td.get("a"));
        assertEquals(2, td.get("b"));
    }

    @Test
    void toEndMapShouldContainTimingFields() {
        TransactionData td = new TransactionData("testTx");
        td.put("collectionId", "banner_xstream");

        Map<String, Object> endMap = td.toEndMap(null);

        assertEquals("testTx", endMap.get("transactionName"));
        assertNotNull(endMap.get("startTime"));
        assertNotNull(endMap.get("endTime"));
        assertNotNull(endMap.get("timeTaken"));
        assertEquals("banner_xstream", endMap.get("collectionId"));
        assertNull(endMap.get("exceptionMessage"));
    }

    @Test
    void toEndMapShouldContainExceptionInfo() {
        TransactionData td = new TransactionData("testTx");
        RuntimeException error = new RuntimeException("something broke");

        Map<String, Object> endMap = td.toEndMap(error);

        assertEquals("something broke", endMap.get("exceptionMessage"));
        assertEquals("java.lang.RuntimeException", endMap.get("exceptionClass"));
    }

    @Test
    void getDataShouldReturnUnmodifiableView() {
        TransactionData td = new TransactionData("testTx");
        td.put("key", "value");

        Map<String, Object> data = td.getData();
        assertThrows(UnsupportedOperationException.class, () -> data.put("x", "y"));
    }
}
