package lxp.httserver.utils;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderValues;

import java.net.InetSocketAddress;

public class WriteResponseUtils {
    public static void writeResponse(ChannelHandlerContext ctx, final FullHttpResponse response) {
        response.headers().set(Constants.HTTP_HEADER.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        ChannelFuture channelFuture = ctx.writeAndFlush(response);
        channelFuture.addListener(new MyChannelFutureListener());
    }

    public static void writeResponseAndClose(ChannelHandlerContext ctx, final FullHttpResponse response) {
        response.headers().set(Constants.HTTP_HEADER.CONNECTION, HttpHeaderValues.CLOSE);
        ChannelFuture channelFuture = ctx.writeAndFlush(response);
        channelFuture.addListener(ChannelFutureListener.CLOSE);
        channelFuture.addListener(new MyChannelFutureListener());
    }

    private static final class MyChannelFutureListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture channelFuture) {
            if (!channelFuture.isSuccess()) {
                InetSocketAddress address = (InetSocketAddress) channelFuture.channel().remoteAddress();
                String ip = address.getAddress().getHostAddress();
                int port = address.getPort();
                LoggerUtils.HTTP_SERVER_LOG.warn("write response to client[" + ip + ":" + port + "] error", channelFuture.cause());
            }
        }
    }
}
