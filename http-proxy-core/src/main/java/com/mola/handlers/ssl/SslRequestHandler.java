package com.mola.handlers.ssl;

import com.mola.pool.SslEncryptionChannelPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: ssl 数据转发
 * @date : 2023-09-30 09:10
 **/

public class SslRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(SslRequestHandler.class);

    private final Channel channel;

    public SslRequestHandler(Channel client2EncrytionChannel, Bootstrap bootstrap,
                             String host, int port) {
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            if (!future.isSuccess()) {
                throw new RuntimeException("ReverseProxyServer start failed!");
            }
            channel = future.channel();
            SslHandler sslHandler = channel.pipeline().get(SslHandler.class);
            sslHandler.handshakeFuture().addListener(future1 -> {
                channel.pipeline().addLast(new SslResponseHandler(client2EncrytionChannel));
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        SslEncryptionChannelPool.instance().removeBothEnd(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channel.close();
        SslEncryptionChannelPool.instance().removeBothEnd(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channel.writeAndFlush(msg);
    }
}
