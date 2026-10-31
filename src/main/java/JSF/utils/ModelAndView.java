package JSF.utils;

import java.util.HashMap;
import java.util.Map;

public class ModelAndView {

    private String view;
    private final Map<String, Object> data = new HashMap<>();

    public ModelAndView() {
    }

    public ModelAndView(String view, Object data) {
        this.view = view;
        this.data.put("data", data);
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public void addObject(String name, Object value) {
        data.put(name, value);
    }

    public Map<String, Object> getData() {
        return data;
    }
}
