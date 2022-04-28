package lxp.httserver.core;

import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AsyncRequest {
    protected Map<String, Object> attributes = new ConcurrentHashMap<>(16);

    /**
     * Request Protocol
     */
    public abstract String getScheme();

    /**
     * 请求path加完整url参数，比如/test?a=xxx&b=xxx
     */
    public abstract String getURIAndQueryString();

    /**
     * 请求路径，比如/test
     */
    public abstract String getRequestURI();

    public abstract byte[] getBody();

    /**
     * 获取客户端IP，如果没有经过代理，则和tcpSourceIp一样
     */
    public abstract String getRemoteAddress();

    /**
     * 获取TCP连接对端IP
     */
    public abstract String getTcpSourceIp();

    public abstract String getMethod();

    public abstract String getLocalAddress();

    public abstract int getLocalPort();

    public abstract String getHeader(String headerName);

    public abstract void setHeader(String headerName, String value);

    public abstract void addHeader(String HeaderName, String value);

    public abstract HttpHeaders getHttpHeaders();

    public abstract int getHeaderSize();

    public abstract int getBodySize();

    public abstract String getParam(String paramName);

    public abstract List<String> getParamList(String paramName);

    /**
     * 获取请求参数，包括url参数和表单参数
     */
    public abstract Map<String, List<String>> getParamMap();

    public abstract void clear();

    public String getCookie(String name) {
        if (name == null) {
            return null;
        }

        String cookieHeader = getHeader("Cookie");
        if (cookieHeader != null) {
            String[] cookieArr = cookieHeader.split(";");
            for (String cookie : cookieArr) {
                String[] tmpArr = cookie.split("=");
                if (tmpArr.length == 2) {
                    String cookieName = tmpArr[0].trim();
                    if (name.equals(cookieName)) {
                        return tmpArr[1];
                    }
                }
            }
        }

        return null;
    }

    public void setAttributes(String name, Object value) {
        attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[0]);
    }
}
