package com.mola.proxy.core.entity;

import com.mola.proxy.core.handlers.http.HttpRequestHandler;
import io.netty.channel.EventLoopGroup;

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
