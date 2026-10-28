package JSF;

// import java.util.ArrayList;
import java.util.HashMap;
// import java.util.List;
import java.util.Map;

import JSF.utils.*;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class LiliFWContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        String packageLocation = context.getInitParameter("package");

        // Dossier de stockage des fichiers televerses : variable, configure ici
        // (param "uploadPath" du web.xml) ; voir lilifw.utils.Uploads.
        Uploads.configure(context.getInitParameter(Uploads.INIT_PARAM));
        context.setAttribute("uploadPath", Uploads.getUploadPath());

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
