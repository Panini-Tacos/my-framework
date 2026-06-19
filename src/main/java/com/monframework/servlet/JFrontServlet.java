package com.monframework.servlet;

import annotations.JController;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utils.JComponentScanner;

import java.io.IOException;
import java.util.List;

public class JFrontServlet extends HttpServlet{

    private String controllersPackage;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        controllersPackage = config.getInitParameter("controllers-package");
        if (controllersPackage == null || controllersPackage.isEmpty()) {
            controllersPackage = "main.java.controllers";
        }
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = uri.substring(contextPath.length());

        if (path.equals("/api")) {
            handleApi(response);
        } else {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("URL interceptee : " + path);
        }
    }

    private void handleApi(HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        try {
            JComponentScanner scanner = new JComponentScanner();
            List<Class<?>> allClasses = scanner.scanPackage(controllersPackage);
            List<Class<?>> controllers = scanner.filterControllers(allClasses);

            response.getWriter().println("=== Liste des Controllers ===");
            if (controllers.isEmpty()) {
                response.getWriter().println("Aucun controller trouve dans le package : " + controllersPackage);
            } else {
                for (Class<?> c : controllers) {
                    response.getWriter().println(" - " + c.getSimpleName());
                }
            }
        } catch (Exception e) {
            response.getWriter().println("Erreur lors du scan : " + e.getMessage());
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
}
