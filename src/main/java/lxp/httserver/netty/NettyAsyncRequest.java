package lxp.httserver.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.QueryStringDecoder;
import lxp.httserver.core.AsyncRequest;
import lxp.httserver.utils.HeaderUtil;
import lxp.httserver.utils.LoggerUtils;
import lxp.httserver.utils.NettyUtils;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import static lxp.httserver.utils.Constants.*;

public class NettyAsyncRequest extends AsyncRequest {
    private byte[] body;
    private HttpHeaders headers;
    private String httpMethod;
    private String tcpSourceIp;
    private String proxyProtocolSourceIp;
    private String localAddress;
    private int localPort;
    private int headerSize;
    private int bodySize = 0;
    private Map<String, List<String>> paramMap;
    private String path;
    private String uri;
    private String scheme;

    public NettyAsyncRequest(ChannelHandlerContext ctx, FullHttpRequest fullHttpRequest) {
        // 通过Proxy Protocol协议获取客户端IP
        proxyProtocolSourceIp = NettyUtils.getChannelAttr(ctx.channel(), CHANNEL_ATTR_KEY.SOURCE_ADDR);

        // 获取TCP连接对端IP
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        tcpSourceIp = remoteSocketAddress.getAddress().getHostAddress();

        // 获取本机IP端口
        InetSocketAddress localSocketAddress = (InetSocketAddress) ctx.channel().localAddress();
        localAddress = localSocketAddress.getAddress().getHostAddress();
        localPort = localSocketAddress.getPort();

        httpMethod = fullHttpRequest.method().toString();

        // uri和path
        uri = fullHttpRequest.uri();
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(uri);
        path = queryStringDecoder.path();

        // header和header size
        headers = fullHttpRequest.headers();
        headerSize = HeaderUtil.headerSize(headers);

        // 读取body
        if (fullHttpRequest.content() != null) {
            bodySize = fullHttpRequest.content().readableBytes();
            body = new byte[bodySize];
            fullHttpRequest.content().readBytes(body);
        }

        Map<String, List<String>> urlParam = null;
        try {
            urlParam = queryStringDecoder.parameters();
        } catch (Exception e) {
            LoggerUtils.HTTP_SERVER_LOG.warn("parse url params error:", e);
        }

        paramMap = mergeParams(urlParam, fullHttpRequest);
    }

    /**
     * 合并URL参数和表单参数
     */
    private Map<String, List<String>> mergeParams(Map<String, List<String>> urlParams, FullHttpRequest request) {
        String contentType = request.headers().get(HTTP_HEADER.CONTENT_TYPE);
        if ("POST".equals(getMethod()) && contentType != null && contentType.contains("x-www-form-urlencoded")) {
            String body = new String(getBody());
            QueryStringDecoder decoder = new QueryStringDecoder(body, false);
            Map<String, List<String>> bodyParams;

            try {
                bodyParams = decoder.parameters();
            } catch (Exception e) {
                LoggerUtils.HTTP_SERVER_LOG.warn("parse body params error:" + e + ",body:" + body);
                return urlParams;
            }

            if (urlParams == null || urlParams.isEmpty()) {
                return bodyParams;
            }

            if (bodyParams != null && !bodyParams.isEmpty()) {
                urlParams.putAll(bodyParams);
            }
        }

        return urlParams;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getRequestURI() {
        return path;
    }

    @Override
    public String getURIAndQueryString() {
        return uri;
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public String getRemoteAddress() {
        return proxyProtocolSourceIp == null ? tcpSourceIp : proxyProtocolSourceIp;
    }

    @Override
    public String getTcpSourceIp() {
        return tcpSourceIp;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public String getLocalAddress() {
        return localAddress;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    @Override
    public String getHeader(String headerName) {
        return headers.get(headerName);
    }

    @Override
    public void setHeader(String headerName, String value) {
        headers.set(headerName, value);
    }

    @Override
    public void addHeader(String HeaderName, String value) {
        headers.add(HeaderName, value);
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return headers;
    }

    @Override
    public int getHeaderSize() {
        return headerSize;
    }

    @Override
    public int getBodySize() {
        return bodySize;
    }

    @Override
    public String getParam(String paramName) {
        if (paramMap == null) {
            return null;
        }

        List<String> params = paramMap.get(paramName);
        if (params != null && params.size() > 0) {
            return params.get(0);
        }

        return null;
    }

    @Override
    public List<String> getParamList(String paramName) {
        return paramMap == null ? null : paramMap.get(paramName);
    }

    @Override
    public Map<String, List<String>> getParamMap() {
        return paramMap;
    }

    @Override
    public void clear() {
        this.body = null;
        this.headers = null;
        this.path = null;
        this.paramMap = null;
        this.uri = null;
        this.httpMethod = null;
        this.tcpSourceIp = null;
        this.proxyProtocolSourceIp = null;
        this.localAddress = null;
        this.attributes = null;
    }
}
