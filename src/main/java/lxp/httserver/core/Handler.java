package lxp.httserver.core;

import io.netty.channel.ChannelHandlerContext;

public interface Handler {
    void process(AsyncRequest request, AsyncResponse response);

    void onConnected(ChannelHandlerContext ctx);

    void shutdown();
}
