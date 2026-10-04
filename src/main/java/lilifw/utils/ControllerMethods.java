package lilifw.utils;


import java.lang.reflect.Method;

public class ControllerMethods {
    public String controllerName;
    public Method Methode;
    
    public ControllerMethods(String controllerName, Method methode) {
        this.controllerName = controllerName;
        Methode = methode;
    }
    public String getControllerName() {
        return controllerName;
    }
    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }
    public Method getMethode() {
        return Methode;
    }
    public void setMethode(Method methode) {
        Methode = methode;
    }

    public void print(){
        System.out.println(getControllerName() + getMethode().getName());
    }
    
}
