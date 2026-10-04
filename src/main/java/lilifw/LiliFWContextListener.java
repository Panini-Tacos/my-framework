package lilifw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lilifw.utils.*;

public class LiliFWContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String packageLocation = context.getInitParameter("package");

        Map<URLMethod, ControllerMethods> urlToMethods = new HashMap<>();
        Map<URLMethod,ApiMethods>  urlToApiMethods = new HashMap<>();

        try {
            Util.scanAllAnnotedControllers(packageLocation, urlToMethods,urlToApiMethods);
            context.setAttribute("urlToMethods", urlToMethods);
            context.setAttribute("urlToApiMethods", urlToApiMethods);
        } catch (Exception e) {
            throw new RuntimeException("Erreur au scan des controleurs: " + e.getMessage(), e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}
