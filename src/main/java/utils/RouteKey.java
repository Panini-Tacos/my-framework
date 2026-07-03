package utils;

import java.util.Objects;

public class RouteKey {
    private String url;
    private HttpMethod method;

    public RouteKey(String url, HttpMethod method) {
        this.url = url;
        this.method = method;
    }

    public String getUrl() { return url; }
    public HttpMethod getMethod() { return method; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouteKey routeKey = (RouteKey) o;
        return Objects.equals(url, routeKey.url) && method == routeKey.method;
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, method);
    }
}
