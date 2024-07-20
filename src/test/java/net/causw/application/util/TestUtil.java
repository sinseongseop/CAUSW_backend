package net.causw.application.util;

import java.lang.reflect.Field;

public class TestUtil {

    // 반드시 ID가 존재해야만 하는 상황에서만 외부에서 강제 주입
    public static void setId(Object target, String fieldName, Object value) {
        try {
            Field field = getField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // 본인 객체를 먼저 탐색 한후 일치하는 필드가 없으면 부모 필드를 탐색
    private static Field getField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found");
    }

}
