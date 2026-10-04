package lilifw;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import lilifw.annotation.WebAPI;
import lilifw.utils.ApiMethods;
import lilifw.utils.ControllerMethods;
import lilifw.utils.JsonSerializer;
import lilifw.utils.ModelAndView;
import lilifw.utils.URLMethod;

public class FrontControllerServlet extends HttpServlet {

    Map<URLMethod, ControllerMethods> urlToMethods;
    Map<URLMethod, ApiMethods> urlToApiMethods = new HashMap<>();

    private ApplicationContext ctx;

    @SuppressWarnings("unchecked")
    public void init() throws ServletException {
        urlToMethods = (Map<URLMethod, ControllerMethods>) getServletContext().getAttribute("urlToMethods");
        urlToApiMethods = (Map<URLMethod, ApiMethods>) getServletContext().getAttribute("urlToApiMethods");

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
            // TODO Auto-generated catch block
            throw new ServletException(e.getMessage(), e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            processRequest(request, response);
        } catch (Exception e) {
            // TODO Auto-generated catch block
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

        // D'abord verifier si la methode existe dans urlToApiMethods
        ApiMethods foncApideURL = urlToApiMethods.get(new URLMethod(url, request.getMethod()));
        if (foncApideURL != null) {
            // retourner du json
            try {
                // Chargement de la classe pour prendre en compte ApplicationContext
                Class<?> controllerClass = Class.forName(foncApideURL.getController());
                Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

                Class<?>[] parameterTypes = foncApideURL.getMethode().getParameterTypes();
                Object[] parameters = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].equals(ApplicationContext.class)) {
                        parameters[i] = ctx;
                    } else {
                        parameters[i] = null;
                    }
                }

                // invoker la methode API 
                Object result = foncApideURL.getMethode().invoke(controllerInstance, parameters);

                // verifier si le developpeur a mis true ou false dans le mode de retour JSON
                if (foncApideURL.isToJson()) {
                    response.setContentType("application/json; charset=UTF-8");
                    response.getWriter().write(JsonSerializer.serialize(result));
                } else {
                    response.setContentType("text/plain; charset=UTF-8");
                    response.getWriter().write(String.valueOf(result));
                }
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
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

            Class<?> controllerClass = Class.forName(foncDeURL.getControllerName());
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();

            Class<?>[] parameterTypes = foncDeURL.getMethode().getParameterTypes();
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i].equals(ApplicationContext.class)) {
                    parameters[i] = ctx;
                } else {
                    parameters[i] = null;
                }
            }

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

    // public Map<String, ControllerMethods> getUrlToMethods() {
    // return urlToMethods;
    // }

    // public Map<String, List<Method>> getListeMethodesAnnotes() {
    // return listeMethodesAnnotes;
    // }

}