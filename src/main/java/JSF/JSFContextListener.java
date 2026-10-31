package JSF;

import java.util.HashMap;
import java.util.Map;

import JSF.utils.ApiMethods;
import JSF.utils.ControllerMethods;
import JSF.utils.URLMethod;
import JSF.utils.Uploads;
import JSF.utils.Util;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class JSFContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String packageLocation = context.getInitParameter("package");

        Uploads.configure(context.getInitParameter(Uploads.INIT_PARAM));
        context.setAttribute("uploadPath", Uploads.getUploadPath());

        Map<URLMethod, ControllerMethods> urlToMethods = new HashMap<>();
        Map<URLMethod, ApiMethods> urlToApiMethods = new HashMap<>();

        try {
            Util.scanAllAnnotedControllers(packageLocation, urlToMethods, urlToApiMethods);
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
