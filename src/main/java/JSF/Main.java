package JSF;

import java.util.HashMap;
import java.util.Map;

import JSF.utils.ApiMethods;
import JSF.utils.ControllerMethods;
import JSF.utils.URLMethod;
import JSF.utils.Util;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: JSF.Main <package>");
            return;
        }

        Map<URLMethod, ControllerMethods> routes = new HashMap<>();
        Map<URLMethod, ApiMethods> apis = new HashMap<>();

        Util.scanAllAnnotedControllers(args[0], routes, apis);

        for (URLMethod route : routes.keySet()) {
            ControllerMethods target = routes.get(route);
            System.out.println(route.getMethod() + " " + route.getUrl() + " -> "
                    + target.getControllerName() + "." + target.getMethode().getName());
        }

        for (URLMethod route : apis.keySet()) {
            ApiMethods target = apis.get(route);
            System.out.println(route.getMethod() + " " + route.getUrl() + " -> "
                    + target.getController() + "." + target.getMethode().getName());
        }
    }
}
