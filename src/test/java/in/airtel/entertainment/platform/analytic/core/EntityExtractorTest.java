package in.airtel.entertainment.platform.analytic.core;

import in.airtel.entertainment.platform.analytic.annotation.Analysed;
import in.airtel.entertainment.platform.analytic.annotation.AnalysedEntity;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EntityExtractorTest {

    @AnalysedEntity(name = "testBean")
    static class AnnotatedBean {
        @Analysed(name = "userId")
        private String id = "user-123";

        @Analysed
        public String name = "John";

        public String notAnnotated = "skip-me";

        @Analysed(name = "itemCount")
        public int getCount() {
            return 42;
        }
    }

    @AnalysedEntity
    static class NoAnnotatedMembersBean {
        public String publicField = "visible";
        public int publicInt = 10;
        private String privateField = "hidden";
    }

    @Test
    void shouldExtractAnnotatedFields() {
        Map<String, Object> result = EntityExtractor.extract(new AnnotatedBean());

        assertEquals("user-123", result.get("userId"));
        assertEquals("John", result.get("name"));
        assertEquals(42, result.get("itemCount"));
        assertFalse(result.containsKey("notAnnotated"));
    }

    @Test
    void shouldFallBackToPublicPrimitivesWhenNoAnnotation() {
        Map<String, Object> result = EntityExtractor.extract(new NoAnnotatedMembersBean());

        assertEquals("visible", result.get("publicField"));
        assertEquals(10, result.get("publicInt"));
        assertFalse(result.containsKey("privateField"));
    }

    @Test
    void shouldReturnEmptyMapForNull() {
        Map<String, Object> result = EntityExtractor.extract(null);
        assertTrue(result.isEmpty());
    }
}
