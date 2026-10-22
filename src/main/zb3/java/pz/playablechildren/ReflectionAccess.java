package pz.playablechildren;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Cached reflective access for the few private fields not exposed by B42.20.4. */
public final class ReflectionAccess {
    private record FieldKey(Class<?> type, String name) {}
    private record MethodKey(Class<?> type, String name, List<Class<?>> parameters) {}

    private static final ConcurrentHashMap<FieldKey, Optional<Field>> FIELDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<MethodKey, Optional<Method>> METHODS = new ConcurrentHashMap<>();

    private ReflectionAccess() {}

    public static Field field(Class<?> type, String name) {
        if (type == null || name == null) return null;
        return FIELDS.computeIfAbsent(new FieldKey(type, name), ReflectionAccess::findField).orElse(null);
    }

    private static Optional<Field> findField(FieldKey key) {
        for (Class<?> current = key.type(); current != null; current = current.getSuperclass()) {
            try {
                Field result = current.getDeclaredField(key.name());
                result.setAccessible(true);
                return Optional.of(result);
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable failure) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Method method(Class<?> type, String name, Class<?>... parameterTypes) {
        if (type == null || name == null) return null;
        List<Class<?>> parameters = List.of(parameterTypes.clone());
        MethodKey key = new MethodKey(type, name, parameters);
        return METHODS.computeIfAbsent(key, ReflectionAccess::findMethod).orElse(null);
    }

    private static Optional<Method> findMethod(MethodKey key) {
        Class<?>[] parameters = key.parameters().toArray(Class<?>[]::new);
        for (Class<?> current = key.type(); current != null; current = current.getSuperclass()) {
            try {
                Method result = current.getDeclaredMethod(key.name(), parameters);
                result.setAccessible(true);
                return Optional.of(result);
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable failure) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static Object tryGet(Object target, String name, Object fallback) {
        if (target == null || name == null) return fallback;
        try {
            Field field = field(target.getClass(), name);
            return field == null ? fallback : field.get(target);
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    public static boolean trySet(Object target, String name, Object value) {
        if (target == null || name == null) return false;
        try {
            Field field = field(target.getClass(), name);
            if (field == null) return false;
            field.set(target, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
