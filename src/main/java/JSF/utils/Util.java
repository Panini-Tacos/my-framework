package JSF.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Map;

import JSF.annotation.Controller;
import JSF.annotation.UrlMapping;
import JSF.annotation.WebAPI;

public final class Util {

    private Util() {
    }

    public static void scanAllAnnotedControllers(String packageLocation,
            Map<URLMethod, ControllerMethods> urlToMethods,
            Map<URLMethod, ApiMethods> urlToApiMethods) throws Exception {

        packageLocation = packageLocation.replace(".", "/") + "/controllers";
        URL url = Thread.currentThread().getContextClassLoader().getResource(packageLocation);

        if (url == null) {
            System.out.println("Pas de Controller dans : " + packageLocation);
            return;
        }

        File dossier = new File(url.toURI());
        File[] fichiers = dossier.listFiles();
        if (fichiers == null) {
            return;
        }

        for (File file : fichiers) {
            if (!file.isFile() || !file.getName().endsWith(".class")) {
                continue;
            }

            String className = packageLocation.replace("/", ".") + "." + file.getName().replace(".class", "");
            Class<?> clazz = Class.forName(className);

            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }

            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(UrlMapping.class)) {
                    UrlMapping mapping = method.getAnnotation(UrlMapping.class);
                    URLMethod reqUrlMethod = new URLMethod(mapping.value(), mapping.method());

                    if (urlToMethods.containsKey(reqUrlMethod)) {
                        ControllerMethods existing = urlToMethods.get(reqUrlMethod);
                        throw new Exception("Conflit d'URL : '" + mapping.value() + "' est deja utilise par "
                                + existing.getControllerName() + "." + existing.getMethode().getName()
                                + " et par " + clazz.getName() + "." + method.getName());
                    }

                    urlToMethods.put(reqUrlMethod, new ControllerMethods(clazz.getName(), method));
                } else if (method.isAnnotationPresent(WebAPI.class)) {
                    WebAPI api = method.getAnnotation(WebAPI.class);
                    URLMethod reqApiMethod = new URLMethod(api.api(), "GET");

                    if (urlToApiMethods.containsKey(reqApiMethod)) {
                        ApiMethods existing = urlToApiMethods.get(reqApiMethod);
                        throw new Exception("Conflit d'URL API : '" + api.api() + "' est deja utilisee par "
                                + existing.getController() + "." + existing.getMethode().getName()
                                + " et par " + clazz.getName() + "." + method.getName());
                    }

                    for (URLMethod existingKey : urlToMethods.keySet()) {
                        if (existingKey.getUrl().equals(api.api())) {
                            ControllerMethods existing = urlToMethods.get(existingKey);
                            throw new Exception("Conflit d'URL : '" + api.api() + "' est deja utilisee par "
                                    + existing.getControllerName() + "." + existing.getMethode().getName()
                                    + " et par " + clazz.getName() + "." + method.getName());
                        }
                    }

                    urlToApiMethods.put(reqApiMethod, new ApiMethods(clazz.getName(), method, api.toJSON()));
                }
            }
        }
    }
}
