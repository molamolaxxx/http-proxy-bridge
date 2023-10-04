package com.mola.socks5;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kdyzm
 * @date 2021-04-24
 */
public class HttpServer2ClientInboundHandler extends ChannelInboundHandlerAdapter {


    private static final Logger log = LoggerFactory.getLogger(HttpServer2ClientInboundHandler.class);

    private final ChannelHandlerContext clientChannelHandlerContext;

    public HttpServer2ClientInboundHandler(ChannelHandlerContext clientChannelHandlerContext) {
        this.clientChannelHandlerContext = clientChannelHandlerContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        clientChannelHandlerContext.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        clientChannelHandlerContext.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HttpServer2ClientInboundHandler exception", cause);
        clientChannelHandlerContext.channel().close();
    }
}
