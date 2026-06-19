package utils;
import annotations.JController;
import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class JComponentScanner {

    public List<Class<?>> scanPackage(String packageToScan) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        
        String path = packageToScan.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL resource = classLoader.getResource(path);
        
        if (resource == null) {
            throw new IllegalArgumentException("Le package " + packageToScan + " n'existe pas.");
        }
        
        File directory = new File(resource.getFile());
        if (directory.exists() && directory.isDirectory()) {
            scanDirectory(directory, packageToScan, classes, classLoader);
        }
        
        return classes;
    }

    private void scanDirectory(File directory, String packageName, List<Class<?>> classes, ClassLoader classLoader) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), classes, classLoader);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." + file.getName().replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    // skip
                }
            }
        }
    }

    public List<Class<?>> filterControllers(List<Class<?>> allClasses) {
        List<Class<?>> controllers = new ArrayList<>();
        
        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent((Class<? extends Annotation>) JController.class)) {
                controllers.add(clazz);
            }
        }
            
        return controllers;
    }
}
