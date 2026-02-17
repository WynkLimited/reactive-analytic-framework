package in.airtel.entertainment.platform.analytic.core;

import in.airtel.entertainment.platform.analytic.annotation.Analysed;
import in.airtel.entertainment.platform.analytic.annotation.AnalysedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(EntityExtractor.class);
    private static final ConcurrentHashMap<Class<?>, List<MemberAccessor>> CACHE = new ConcurrentHashMap<>();

    private EntityExtractor() {
    }

    public static Map<String, Object> extract(Object entity) {
        if (entity == null) {
            return Collections.emptyMap();
        }
        Class<?> clazz = entity.getClass();
        List<MemberAccessor> accessors = CACHE.computeIfAbsent(clazz, EntityExtractor::buildAccessors);
        Map<String, Object> result = new LinkedHashMap<>();
        for (MemberAccessor accessor : accessors) {
            try {
                Object value = accessor.getValue(entity);
                if (value != null) {
                    result.put(accessor.name, value);
                }
            } catch (Exception e) {
                LOG.warn("Failed to extract field {}: {}", accessor.name, e.getMessage());
            }
        }
        return result;
    }

    private static List<MemberAccessor> buildAccessors(Class<?> clazz) {
        List<MemberAccessor> accessors = new ArrayList<>();
        boolean hasAnnotatedMembers = false;

        // Scan fields
        for (Field field : getAllFields(clazz)) {
            Analysed analysed = field.getAnnotation(Analysed.class);
            if (analysed != null) {
                hasAnnotatedMembers = true;
                String name = analysed.name().isEmpty() ? field.getName() : analysed.name();
                field.setAccessible(true);
                accessors.add(new FieldAccessor(name, field));
            }
        }

        // Scan methods (zero-arg, non-void)
        for (Method method : clazz.getDeclaredMethods()) {
            Analysed analysed = method.getAnnotation(Analysed.class);
            if (analysed != null && method.getParameterCount() == 0
                    && method.getReturnType() != void.class) {
                hasAnnotatedMembers = true;
                String name = analysed.name().isEmpty() ? deriveNameFromMethod(method) : analysed.name();
                method.setAccessible(true);
                accessors.add(new MethodAccessor(name, method));
            }
        }

        // If no @Analysed members, take all public primitive fields
        if (!hasAnnotatedMembers) {
            accessors.clear();
            for (Field field : getAllFields(clazz)) {
                if (Modifier.isPublic(field.getModifiers()) && isPrimitive(field.getType())) {
                    accessors.add(new FieldAccessor(field.getName(), field));
                }
            }
        }

        return accessors;
    }

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Collections.addAll(fields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return fields;
    }

    private static String deriveNameFromMethod(Method method) {
        String name = method.getName();
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return name;
    }

    private static boolean isPrimitive(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || clazz == Boolean.class
                || clazz == Character.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Float.class
                || clazz == Double.class;
    }

    private static abstract class MemberAccessor {
        final String name;

        MemberAccessor(String name) {
            this.name = name;
        }

        abstract Object getValue(Object target) throws Exception;
    }

    private static class FieldAccessor extends MemberAccessor {
        private final Field field;

        FieldAccessor(String name, Field field) {
            super(name);
            this.field = field;
        }

        @Override
        Object getValue(Object target) throws Exception {
            return field.get(target);
        }
    }

    private static class MethodAccessor extends MemberAccessor {
        private final Method method;

        MethodAccessor(String name, Method method) {
            super(name);
            this.method = method;
        }

        @Override
        Object getValue(Object target) throws Exception {
            return method.invoke(target);
        }
    }
}
