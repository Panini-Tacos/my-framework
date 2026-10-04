package lilifw.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class JsonSerializer {

    public static String serialize(Object object) throws Exception {
        return toJson(object, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private static String toJson(Object object, Set<Object> visited) throws Exception {
        if (object == null) {
            return "null";
        }
        Class<?> type = object.getClass();
        if (isPrimitiveLike(type)) {
            return toPrimitive(object);
        }
        if (type.isEnum()) {
            return quote(((Enum<?>) object).name());
        }
        if (object instanceof CharSequence) {
            return quote(object.toString());
        }
        if (object instanceof Collection) {
            return toJsonArray(((Collection<?>) object).toArray(), visited);
        }
        if (type.isArray()) {
            return toJsonArray(object, visited);
        }
        if (object instanceof Map) {
            return toJsonMap((Map<?, ?>) object, visited);
        }
        return toJsonObject(object, visited);
    }

    private static boolean isPrimitiveLike(Class<?> type) {
        return type.isPrimitive()
                || Number.class.isAssignableFrom(type)
                || type.equals(Boolean.class)
                || type.equals(Character.class);
    }

    private static String toPrimitive(Object object) {
        if (object instanceof Character) {
            return quote(object.toString());
        }
        return object.toString();
    }

    private static String toJsonArray(Object array, Set<Object> visited) throws Exception {
        int length = Array.getLength(array);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(toJson(Array.get(array, i), visited));
        }
        return sb.append("]").toString();
    }

    private static String toJsonMap(Map<?, ?> map, Set<Object> visited) throws Exception {
        checkCycle(map, visited);
        visited.add(map);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(quote(String.valueOf(entry.getKey())))
              .append(":")
              .append(toJson(entry.getValue(), visited));
        }
        visited.remove(map);
        return sb.append("}").toString();
    }

    private static String toJsonObject(Object object, Set<Object> visited) throws Exception {
        checkCycle(object, visited);
        visited.add(object);
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Method getter : object.getClass().getMethods()) {
            String name = getterName(getter);
            if (name == null) {
                continue;
            }
            Object value = getter.invoke(object);
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append(quote(name)).append(":").append(toJson(value, visited));
        }
        visited.remove(object);
        return sb.append("}").toString();
    }

    private static String getterName(Method method) {
        if (method.getParameterCount() != 0 || method.getReturnType().equals(Void.TYPE)) {
            return null;
        }
        String name = method.getName();
        if (name.equals("getClass")) {
            return null;
        }
        if (name.startsWith("get") && name.length() > 3) {
            return Character.toLowerCase(name.charAt(3)) + name.substring(4);
        }
        if (name.startsWith("is") && name.length() > 2
                && (method.getReturnType().equals(Boolean.class) || method.getReturnType().equals(boolean.class))) {
            return Character.toLowerCase(name.charAt(2)) + name.substring(3);
        }
        return null;
    }

    private static void checkCycle(Object object, Set<Object> visited) throws Exception {
        if (visited.contains(object)) {
            throw new Exception("Cycle detecte dans la serialisation JSON pour : " + object.getClass().getName());
        }
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append("\"").toString();
    }
}