package com.mola.proxy.core.handlers.socks5;

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
public class HttpServer2ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpServer2ClientInboundHandler.class);

    private final ChannelHandlerContext clientChannelHandlerContext;

    public HttpServer2ClientInboundHandler(ChannelHandlerContext clientChannelHandlerContext) {
        this.clientChannelHandlerContext = clientChannelHandlerContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        clientChannelHandlerContext.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        clientChannelHandlerContext.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HttpServer2ClientInboundHandler exception", cause);
        clientChannelHandlerContext.channel().close();
    }
}
