package JSF.utils;

import java.util.Objects;

public class URLMethod {

    private String url;
    private String method;

    public URLMethod(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        URLMethod urlMethod = (URLMethod) o;
        return Objects.equals(url, urlMethod.url) && Objects.equals(method, urlMethod.method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }
}
