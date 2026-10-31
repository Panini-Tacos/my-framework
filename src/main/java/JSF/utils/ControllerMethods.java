package JSF.utils;

import java.lang.reflect.Method;

public class ControllerMethods {

    private String controllerName;
    private Method methode;

    public ControllerMethods(String controllerName, Method methode) {
        this.controllerName = controllerName;
        this.methode = methode;
    }

    public String getControllerName() {
        return controllerName;
    }

    public void setControllerName(String controllerName) {
        this.controllerName = controllerName;
    }

    public Method getMethode() {
        return methode;
    }

    public void setMethode(Method methode) {
        this.methode = methode;
    }

    public void print() {
        System.out.println(getControllerName() + getMethode().getName());
    }
}
