package lxp.httserver.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class NettyUtils {
    /**
     * 使用非池化ByteBuf分配器
     */
    private static final boolean useUnpooledByteBufAllocator;

    static {
        useUnpooledByteBufAllocator = "true".equals(System.getProperty("httpserver.netty.useUnpooledByteBufAllocator"));
    }

    public static boolean isUseUnpooledByteBufAllocator() {
        return useUnpooledByteBufAllocator;
    }


    /**
     * 获取channel属性
     */
    public static <T> T getChannelAttr(Channel channel, String attr) {
        if (attr == null) {
            return null;
        }

        AttributeKey<T> attributeKey = AttributeKey.valueOf(attr);
        Attribute<T> attribute = channel.attr(attributeKey);

        if (attribute == null) {
            return null;
        }

        return attribute.get();
    }

    public static void setChannelAttr(Channel channel, String attr, String value) {
        if (attr == null) {
            return;
        }

        AttributeKey<Object> attributeKey = AttributeKey.valueOf(attr);
        Attribute<Object> attribute = channel.attr(attributeKey);
        if (attribute != null) {
            attribute.set(value);
        }
    }

    public static void handleException(ChannelHandlerContext ctx, Throwable throwable) {
        // 客户端主动关闭
        if (throwable instanceof java.io.IOException) {
            if (LoggerUtils.HTTP_SERVER_LOG.isDebugEnabled()) {
                InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                String ip = address.getAddress().getHostAddress();
                int port = address.getPort();
                LoggerUtils.HTTP_SERVER_LOG.debug("client[" + ip + ":" + port + "] force disconnect, close channel");
            }
        } else {
            LoggerUtils.HTTP_SERVER_LOG.warn("handleException", throwable);
        }

        // 如果连接可用，把异常原因告诉客户端
        if (ctx.channel().isActive()) {
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            response.headers().set(Constants.HTTP_HEADER.CONTENT_TYPE, "text/plain;charset=utf-8");
            response.content().writeBytes(throwable.getMessage().getBytes(StandardCharsets.UTF_8));
            response.headers().set(Constants.HTTP_HEADER.CONTENT_LENGTH, response.content().readableBytes());
            WriteResponseUtils.writeResponseAndClose(ctx, response);
        } else {
            ctx.close();
        }
    }
}

