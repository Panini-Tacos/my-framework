package lilifw.utils;

import java.lang.reflect.Method;

public class ApiMethods {
    String controller;
    Method methode;
    boolean toJson;
    
    public String getController() {
        return controller;
    }
    public void setController(String controller) {
        this.controller = controller;
    }
    public Method getMethode() {
        return methode;
    }
    public void setMethode(Method methode) {
        this.methode = methode;
    }
    public ApiMethods(String controller, Method methode, boolean toJson) {
        this.controller = controller;
        this.methode = methode;
        this.toJson = toJson;
    }
    public boolean isToJson() {
        return toJson;
    }
    public void setToJson(boolean toJson) {
        this.toJson = toJson;
    }
    
}