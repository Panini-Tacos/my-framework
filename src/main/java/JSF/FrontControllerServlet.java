package JSF;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
// import java.util.List;
// import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.*;

// import jakarta.servlet.http.HttpServletRequest;
// import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import JSF.annotation.Auth;
import JSF.utils.*;


@MultipartConfig(maxFileSize = 20L * 1024 * 1024, maxRequestSize = 30L * 1024 * 1024, fileSizeThreshold = 1024 * 1024)
public class FrontControllerServlet extends HttpServlet {

    Map<URLMethod, ControllerMethods> urlToMethods;
    Map<URLMethod, ApiMethods> urlToApiMethods = new HashMap<>();

    private ApplicationContext ctx;
    private String authSessionKey;

    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        urlToMethods = (Map<URLMethod, ControllerMethods>) getServletContext().getAttribute("urlToMethods");
        urlToApiMethods = (Map<URLMethod, ApiMethods>) getServletContext().getAttribute("urlToApiMethods");
        authSessionKey = AuthSupport.sessionKey(getServletContext());

        try {
            ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext());
        } catch (Exception e) {
            ctx = null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {

            throw new ServletException(e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            throw new ServletException(e.getMessage(), e);
        }
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String url = uri.substring(contextPath.length());

        if (url == null || url.equals("/")) {
            request.setAttribute("annotatedMethods", getUrlToMethods());
            request.getRequestDispatcher("/index.jsp").forward(request, response);
            return;
        }

        request.setAttribute("url", url);

        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            request.getParts();
        }

        ApiMethods foncApideURL = urlToApiMethods.get(new URLMethod(url, request.getMethod()));
        if (foncApideURL != null) {
            Method methodeApi = foncApideURL.getMethode();
            if (!estAutorise(methodeApi, request)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"error\": "
                        + JsonSerializer.serialize("Authentification requise") + "}");
                return;
            }

            try {
                // Chargement de la classe pour prendre en compte ApplicationContext
                Class<?> controllerClass = Class.forName(foncApideURL.getController());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                // Injection des parametres : ApplicationContext, HttpServletRequest,
                // HttpSession et binding des champs de formulaire (donnees simples
                // ou objets).
                Object[] parameters = JSF.utils.DataBinder.bind(methodeApi, request, ctx);

                // invoker la methode API 
                Object result = methodeApi.invoke(controllerInstance, parameters);

                // verifier si le developpeur a mis true ou false dans le mode de retour JSON
                if (foncApideURL.isToJson()) {
                    response.setContentType("application/json; charset=UTF-8");
                    response.getWriter().write(JsonSerializer.serialize(result));
                } else {
                    response.setContentType("text/plain; charset=UTF-8");
                    response.getWriter().write(String.valueOf(result));
                }
            } catch (Exception e) {
                
                Throwable cause = e;
                if (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null) {
                    cause = e.getCause();
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"error\": " + JsonSerializer.serialize(cause.getMessage()) + "}");
            }

        } else {

            // c'est ici qu'on recupere la methode dans la map qu on invokera plus tard
            ControllerMethods foncDeURL = urlToMethods.get(new URLMethod(url, request.getMethod()));
            if (foncDeURL == null) {
                String prefix = url.substring(0, url.lastIndexOf('/'));
                if (prefix.isEmpty())
                    prefix = "/";

                Map<URLMethod, ControllerMethods> matchingRoutes = new HashMap<>();
                for (Map.Entry<URLMethod, ControllerMethods> entry : urlToMethods.entrySet()) {
                    if (entry.getKey().getUrl().startsWith(prefix)) {
                        matchingRoutes.put(entry.getKey(), entry.getValue());
                    }
                }

                request.setAttribute("notFoundUrl", url);
                request.setAttribute("matchingRoutes", matchingRoutes);
                request.getRequestDispatcher("/route.jsp").forward(request, response);
                return;
            }

            if (!estAutorise(foncDeURL.getMethode(), request)) {
                request.setAttribute("authMessage",
                        "Authentification requise pour accéder à cette action.");
                request.setAttribute("authReturnUrl", url);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                request.getRequestDispatcher("/auth.jsp").forward(request, response);
                return;
            }

            Class<?> controllerClass = Class.forName(foncDeURL.getControllerName());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            // Injection des parametres : ApplicationContext, HttpServletRequest,
            // HttpSession et binding des champs de formulaire. Les objets (POJO)
            // sont instancies et remplis a partir des champs du formulaire.
            Object[] parameters = JSF.utils.DataBinder.bind(foncDeURL.getMethode(), request, ctx);

            Object result = foncDeURL.getMethode().invoke(controllerInstance, parameters);

            if (result instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) result;
                for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
                request.getRequestDispatcher("/" + mv.getView() + ".jsp").forward(request, response);
            } else {
                request.setAttribute("controllerName", foncDeURL.getControllerName());
                request.setAttribute("methodName", foncDeURL.getMethode().getName());
                request.getRequestDispatcher("/route.jsp").forward(request, response);
            }
        }

    }

    public Map<URLMethod, ControllerMethods> getUrlToMethods() {
        return urlToMethods;
    }

    /**
     * Vérifie {@link Auth} sur la méthode ou sur sa classe. Une route non
     * annotée est toujours autorisée. Une route annotée exige une session
     * contenant l'attribut configuré par {@code authSessionKey} dans le
     * web.xml.
     */
    private boolean estAutorise(Method methode, HttpServletRequest request) {
        if (methode == null) {
            return true;
        }
        boolean protegee = methode.isAnnotationPresent(Auth.class)
                || methode.getDeclaringClass().isAnnotationPresent(Auth.class);
        if (!protegee) {
            return true;
        }
        return AuthSupport.isAuthenticated(request.getSession(false), authSessionKey);
    }

    // public Map<String, ControllerMethods> getUrlToMethods() {
    // return urlToMethods;
    // }

    // public Map<String, List<Method>> getListeMethodesAnnotes() {
    // return listeMethodesAnnotes;
    // }

}