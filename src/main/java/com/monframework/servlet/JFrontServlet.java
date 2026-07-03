package com.monframework.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.JRoutrRegistry;
import utils.JRoutrRegistry.RouteInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class JFrontServlet extends HttpServlet {

    private JRoutrRegistry registry;
    private String controllersPackage;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllersPackage = config.getInitParameter("controllers-package");
        if (controllersPackage == null || controllersPackage.isEmpty()) {
            controllersPackage = "main.java.controllers";
        }
        registry = new JRoutrRegistry();
        try {
            registry.scanPackage(controllersPackage);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des routes", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();

        RouteInfo exact = registry.findExact(path);
        if (exact != null) {
            out.println("Route trouvee (exact) :");
            out.println("  URL : " + exact.getUrl());
            out.println("  Controller : " + exact.getControllerClass().getName());
            out.println("  Methode : " + exact.getMethod().getName());
            return;
        }

        List<RouteInfo> partials = registry.findStartsWith(path);
        if (!partials.isEmpty()) {
            out.println("Plusieurs routes correspondent a '" + path + "' :");
            for (RouteInfo r : partials) {
                out.println("  " + r.getUrl() + " -> " + r.getControllerClass().getSimpleName() + "." + r.getMethod().getName() + "()");
            }
            return;
        }

        out.println("Aucune route trouvee pour : " + path);
        out.println();
        out.println("=== Routes disponibles ===");
        List<RouteInfo> allRoutes = registry.getAllRoutes();
        if (allRoutes.isEmpty()) {
            out.println("Aucune route enregistree.");
        } else {
            for (RouteInfo r : allRoutes) {
                out.println("  " + r.getUrl() + " -> " + r.getControllerClass().getSimpleName() + "." + r.getMethod().getName() + "()");
            }
        }
    }
}
