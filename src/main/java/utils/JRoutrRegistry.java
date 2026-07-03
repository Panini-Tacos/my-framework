package utils;

import annotations.JController;
import annotations.JRoutrMapping;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class JRoutrRegistry {

    public static class RouteInfo {
        private String url;
        private Class<?> controllerClass;
        private Method method;

        public RouteInfo(String url, Class<?> controllerClass, Method method) {
            this.url = url;
            this.controllerClass = controllerClass;
            this.method = method;
        }

        public String getUrl() { return url; }
        public Class<?> getControllerClass() { return controllerClass; }
        public Method getMethod() { return method; }
    }

    private List<RouteInfo> routes = new ArrayList<>();

    public void scanPackage(String packageName) throws Exception {
        JComponentScanner scanner = new JComponentScanner();
        List<Class<?>> allClasses = scanner.scanPackage(packageName);
        List<Class<?>> controllers = scanner.filterControllers(allClasses);

        for (Class<?> controller : controllers) {
            for (Method method : controller.getDeclaredMethods()) {
                if (method.isAnnotationPresent(JRoutrMapping.class)) {
                    JRoutrMapping mapping = method.getAnnotation(JRoutrMapping.class);
                    routes.add(new RouteInfo(mapping.value(), controller, method));
                }
            }
        }
    }

    public RouteInfo findExact(String url) {
        for (RouteInfo route : routes) {
            if (route.getUrl().equals(url)) {
                return route;
            }
        }
        return null;
    }

    public List<RouteInfo> findStartsWith(String prefix) {
        List<RouteInfo> result = new ArrayList<>();
        for (RouteInfo route : routes) {
            if (route.getUrl().startsWith(prefix)) {
                result.add(route);
            }
        }
        return result;
    }

    public List<RouteInfo> getAllRoutes() {
        return routes;
    }
}
