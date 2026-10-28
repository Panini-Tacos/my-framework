package JSF;

import java.util.HashMap;
import java.util.Map;

import JSF.utils.ControllerMethods;

public class Main {
    public static void main(String[] args) {
        // String url = "jframework";

        Map<String,ControllerMethods> contMethods = new HashMap<>();
        
        try {
            // Util.scanAllAnnotedControllers(url, contMethods);

            for (String string : contMethods.keySet()) {
                System.out.println("url: " + string + " ; ");
                contMethods.get(string).print();
                
            }
        } catch (Exception e) {
            
            e.printStackTrace();
        }
    }
}
