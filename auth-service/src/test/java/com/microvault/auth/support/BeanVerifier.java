package com.microvault.auth.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips every setter/getter pair of a plain bean: the value written through the setter
 * must be the value returned by the matching getter.
 */
public final class BeanVerifier {

    private BeanVerifier() {
    }

    /** Returns the number of setter/getter pairs that were verified. */
    public static int verify(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object bean = constructor.newInstance();
        int verified = 0;
        for (Method setter : type.getMethods()) {
            if (!setter.getName().startsWith("set") || setter.getParameterCount() != 1
                    || Modifier.isStatic(setter.getModifiers())) {
                continue;
            }
            Method getter = findGetter(type, setter);
            assertNotNull(getter, "No getter for " + type.getSimpleName() + "." + setter.getName());
            Object sample = sample(setter.getParameterTypes()[0]);
            setter.invoke(bean, sample);
            assertEquals(sample, getter.invoke(bean), type.getSimpleName() + "." + setter.getName());
            verified++;
        }
        assertTrue(verified > 0, type.getSimpleName() + " has no setters");
        return verified;
    }

    private static Method findGetter(Class<?> type, Method setter) {
        String property = setter.getName().substring(3);
        for (String prefix : new String[] {"get", "is"}) {
            try {
                return type.getMethod(prefix + property);
            } catch (NoSuchMethodException ignored) {
                // try next prefix
            }
        }
        return null;
    }

    private static Object sample(Class<?> parameter) {
        if (parameter == String.class) {
            return "sample";
        }
        if (parameter == UUID.class) {
            return UUID.fromString("11111111-2222-3333-4444-555555555555");
        }
        if (parameter == BigDecimal.class) {
            return new BigDecimal("12.34");
        }
        if (parameter == LocalDate.class) {
            return LocalDate.of(2026, 1, 15);
        }
        if (parameter == LocalDateTime.class) {
            return LocalDateTime.of(2026, 1, 15, 10, 30);
        }
        if (parameter == boolean.class || parameter == Boolean.class) {
            return Boolean.TRUE;
        }
        if (parameter == int.class || parameter == Integer.class) {
            return 7;
        }
        if (parameter == long.class || parameter == Long.class) {
            return 7L;
        }
        if (List.class.isAssignableFrom(parameter)) {
            List<Object> list = new ArrayList<>();
            list.add("item");
            return list;
        }
        try {
            // nested beans: the very same instance must come back from the getter
            return parameter.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalArgumentException("Unsupported setter type " + parameter, exception);
        }
    }
}
