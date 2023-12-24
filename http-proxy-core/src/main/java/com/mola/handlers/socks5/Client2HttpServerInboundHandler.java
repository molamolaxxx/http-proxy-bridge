package com.mola.handlers.socks5;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class Client2HttpServerInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(Client2HttpServerInboundHandler.class);

    private final ChannelFuture httpChannelFuture;

    public Client2HttpServerInboundHandler(ChannelFuture httpChannelFuture) {
        this.httpChannelFuture = httpChannelFuture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        httpChannelFuture.channel().writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        httpChannelFuture.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Client2HttpServerInboundHandler exception", cause);
        httpChannelFuture.channel().close();
    }
}
