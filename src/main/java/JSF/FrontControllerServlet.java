package JSF;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import JSF.annotation.Auth;
import JSF.utils.ApiMethods;
import JSF.utils.AuthSupport;
import JSF.utils.ControllerMethods;
import JSF.utils.DataBinder;
import JSF.utils.JsonSerializer;
import JSF.utils.ModelAndView;
import JSF.utils.URLMethod;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@MultipartConfig(maxFileSize = 20L * 1024 * 1024, maxRequestSize = 30L * 1024 * 1024, fileSizeThreshold = 1024 * 1024)
public class FrontControllerServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private transient Map<URLMethod, ControllerMethods> urlToMethods;
    private transient Map<URLMethod, ApiMethods> urlToApiMethods;
    private transient ApplicationContext ctx;
    private String authSessionKey;
    private String roleSessionKey;

    @Override
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        urlToMethods = (Map<URLMethod, ControllerMethods>) getServletContext().getAttribute("urlToMethods");
        urlToApiMethods = (Map<URLMethod, ApiMethods>) getServletContext().getAttribute("urlToApiMethods");
        authSessionKey = AuthSupport.sessionKey(getServletContext());
        roleSessionKey = AuthSupport.roleSessionKey(getServletContext());

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

    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
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

        // --- API ---

        ApiMethods apiMethods = urlToApiMethods.get(new URLMethod(url, request.getMethod()));
        if (apiMethods != null) {
            Method method = apiMethods.getMethode();
            Acces acces = verifierAcces(method, request);
            if (acces != Acces.AUTORISE) {
                response.setStatus(acces == Acces.NON_AUTHENTIFIE
                        ? HttpServletResponse.SC_UNAUTHORIZED
                        : HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json; charset=UTF-8");
                String message = acces == Acces.NON_AUTHENTIFIE
                        ? "Authentification requise"
                        : "Rôle insuffisant";
                response.getWriter().write("{\"error\": " + JsonSerializer.serialize(message) + "}");
                return;
            }

            try {
                Class<?> controllerClass = Class.forName(apiMethods.getController());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
                Object[] parameters = DataBinder.bind(method, request, ctx);
                Object result = method.invoke(controllerInstance, parameters);

                if (apiMethods.isToJson()) {
                    response.setContentType("application/json; charset=UTF-8");
                    response.getWriter().write(JsonSerializer.serialize(result));
                } else {
                    response.setContentType("text/plain; charset=UTF-8");
                    response.getWriter().write(String.valueOf(result));
                }
            } catch (Exception e) {
                Throwable cause = e;
                if (e instanceof InvocationTargetException && e.getCause() != null) {
                    cause = e.getCause();
                }
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json; charset=UTF-8");
                response.getWriter().write("{\"error\": " + JsonSerializer.serialize(cause.getMessage()) + "}");
            }
            return;
        }

        // --- JSP ---

        ControllerMethods controllerMethods = urlToMethods.get(new URLMethod(url, request.getMethod()));
        if (controllerMethods == null) {
            int lastSlash = url.lastIndexOf('/');
            String prefix = lastSlash > 0 ? url.substring(0, lastSlash) : "/";

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

        Method method = controllerMethods.getMethode();
        Acces acces = verifierAcces(method, request);
        if (acces != Acces.AUTORISE) {
            String message = acces == Acces.NON_AUTHENTIFIE
                    ? "Authentification requise pour accéder à cette action."
                    : "Rôle insuffisant pour accéder à cette action.";
            request.setAttribute("authMessage", message);
            request.setAttribute("authReturnUrl", url);
            response.setStatus(acces == Acces.NON_AUTHENTIFIE
                    ? HttpServletResponse.SC_UNAUTHORIZED
                    : HttpServletResponse.SC_FORBIDDEN);
            request.getRequestDispatcher("/auth.jsp").forward(request, response);
            return;
        }

        Class<?> controllerClass = Class.forName(controllerMethods.getControllerName());
        Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        Object[] parameters = DataBinder.bind(method, request, ctx);
        Object result = method.invoke(controllerInstance, parameters);

        if (result instanceof ModelAndView) {
            ModelAndView mv = (ModelAndView) result;
            for (Map.Entry<String, Object> entry : mv.getData().entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            request.getRequestDispatcher("/" + mv.getView() + ".jsp").forward(request, response);
        } else {
            request.setAttribute("controllerName", controllerMethods.getControllerName());
            request.setAttribute("methodName", method.getName());
            request.getRequestDispatcher("/route.jsp").forward(request, response);
        }
    }

    public Map<URLMethod, ControllerMethods> getUrlToMethods() {
        return urlToMethods;
    }

    // --- Accès ---

    private enum Acces {
        AUTORISE,
        NON_AUTHENTIFIE,
        ROLE_INSUFFISANT
    }

    private Acces verifierAcces(Method method, HttpServletRequest request) {
        if (method == null) {
            return Acces.AUTORISE;
        }

        Auth auth = method.getAnnotation(Auth.class);
        if (auth == null) {
            auth = method.getDeclaringClass().getAnnotation(Auth.class);
        }
        if (auth == null) {
            return Acces.AUTORISE;
        }

        HttpSession session = request.getSession(false);
        if (!AuthSupport.isAuthenticated(session, authSessionKey)) {
            return Acces.NON_AUTHENTIFIE;
        }

        String[] roles = auth.role();
        if (roles.length > 0 && !AuthSupport.hasAnyRole(session, roleSessionKey, roles)) {
            return Acces.ROLE_INSUFFISANT;
        }
        return Acces.AUTORISE;
    }
}
