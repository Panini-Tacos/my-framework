package lilifw.utils;

import java.util.Objects;

public class URLMethod {
    String url;
    String Method;
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getMethod() {
        return Method;
    }
    public void setMethod(String method) {
        Method = method;
    }
    public URLMethod(String url, String method) {
        this.url = url;
        Method = method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        URLMethod urlMethod = (URLMethod) o;
        return Objects.equals(url, urlMethod.url) && Objects.equals(Method, urlMethod.Method);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, Method);
    }
}
