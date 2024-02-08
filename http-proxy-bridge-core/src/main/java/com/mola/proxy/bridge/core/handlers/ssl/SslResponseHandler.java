package com.mola.proxy.bridge.core.handlers.ssl;

import com.mola.proxy.bridge.core.pool.SslEncryptionChannelPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: ssl响应处理
 * @date : 2023-10-22 20:47
 **/
public class SslResponseHandler extends ChannelInboundHandlerAdapter {

    private Channel encryptionServerChannel;

    public SslResponseHandler(Channel encryptionServerChannel) {
        this.encryptionServerChannel = encryptionServerChannel;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        SslEncryptionChannelPool.instance().removeBothEnd(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        encryptionServerChannel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        encryptionServerChannel.close();
        SslEncryptionChannelPool.instance().removeBothEnd(ctx.channel());
    }
}
