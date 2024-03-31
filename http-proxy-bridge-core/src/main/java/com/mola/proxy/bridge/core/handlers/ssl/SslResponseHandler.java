package com.mola.proxy.bridge.core.handlers.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: ssl响应处理
 * @date : 2023-10-22 20:47
 **/
public class SslResponseHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SslResponseHandler.class);

    private final Channel encryptionServerChannel;

    public SslResponseHandler(Channel encryptionServerChannel) {
        this.encryptionServerChannel = encryptionServerChannel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("SslResponseHandler error, channel = {}", ctx.channel(), cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        encryptionServerChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        encryptionServerChannel.close();
    }
}
