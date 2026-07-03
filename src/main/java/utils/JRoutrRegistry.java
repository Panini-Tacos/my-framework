package utils;

import annotations.JController;
import annotations.JRoutrMapping;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JRoutrRegistry {

    public static class RouteInfo {
        private String url;
        private HttpMethod httpMethod;
        private Class<?> controllerClass;
        private Method method;

        public RouteInfo(String url, HttpMethod httpMethod, Class<?> controllerClass, Method method) {
            this.url = url;
            this.httpMethod = httpMethod;
            this.controllerClass = controllerClass;
            this.method = method;
        }

        public String getUrl() { return url; }
        public HttpMethod getHttpMethod() { return httpMethod; }
        public Class<?> getControllerClass() { return controllerClass; }
        public Method getMethod() { return method; }
    }

    private Map<RouteKey, RouteInfo> routes = new LinkedHashMap<>();

    public void scanPackage(String packageName) throws Exception {
        JComponentScanner scanner = new JComponentScanner();
        List<Class<?>> allClasses = scanner.scanPackage(packageName);
        List<Class<?>> controllers = scanner.filterControllers(allClasses);

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(JRoutrMapping.class)) {
                    JRoutrMapping mapping = method.getAnnotation(JRoutrMapping.class);
                    RouteKey key = new RouteKey(mapping.url(), mapping.method());

                    if (routes.containsKey(key)) {
                        RouteInfo existing = routes.get(key);
                        throw new Exception(
                            "Conflit de route : [" + mapping.method() + "] " + mapping.url()
                            + " deja associe a " + existing.getControllerClass().getName() + "." + existing.getMethod().getName()
                            + " ne peut pas etre reassigne a " + controller.getName() + "." + method.getName()
                        );
                    }

                    routes.put(key, new RouteInfo(mapping.url(), mapping.method(), controller, method));
                }
            }
        }
    }

    public RouteInfo findExact(String url, HttpMethod method) {
        return routes.get(new RouteKey(url, method));
    }

    public List<RouteInfo> findStartsWith(String prefix) {
        List<RouteInfo> result = new ArrayList<>();
        for (Map.Entry<RouteKey, RouteInfo> entry : routes.entrySet()) {
            if (entry.getKey().getUrl().startsWith(prefix)) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    public List<RouteInfo> getAllRoutes() {
        return new ArrayList<>(routes.values());
    }
}
