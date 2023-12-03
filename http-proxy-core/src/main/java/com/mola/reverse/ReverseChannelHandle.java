package com.mola.reverse;

import com.mola.common.HttpRequestHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-10-01 10:08
 **/
public class ReverseChannelHandle {

    private EventLoopGroup group;

    private HttpRequestHandler httpRequestHandler;

    public ReverseChannelHandle(EventLoopGroup group, HttpRequestHandler httpRequestHandler) {
        this.group = group;
        this.httpRequestHandler = httpRequestHandler;
    }

    public void shutdown() {
        group.shutdownGracefully();
        httpRequestHandler.shutdown();;
    }
}
