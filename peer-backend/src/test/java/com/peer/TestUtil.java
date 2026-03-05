package com.peer;

import java.lang.reflect.Field;

public class TestUtil {

    public static <T> T createDto(Class<T> clazz, Object... fieldValues) {
        try {
            T instance = clazz.getDeclaredConstructor().newInstance();
            for (int i = 0; i < fieldValues.length; i += 2) {
                String fieldName = (String) fieldValues[i];
                Object value = fieldValues[i + 1];
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, value);
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DTO", e);
        }
    }
}
