package com.mola.ssl;

import com.mola.pool.ReverseProxyConnectPool;
import com.mola.pool.SslEncryptionChannelPool;
import com.mola.reverse.ReverseProxyChannelMonitor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 09:10
 **/

public class SslDataTransferHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelMonitor.class);

    private Channel channel;

    public SslDataTransferHandler(Channel client2EncrytionChannel, Bootstrap bootstrap,
                                  String host, int port) {
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            if (!future.isSuccess()) {
                throw new RuntimeException("ReverseProxyServer start failed!");
            }
            channel = future.channel();
//            SslEncryptionChannelPool encryptionChannelPool = SslEncryptionChannelPool.instance();
//            encryptionChannelPool.mapping(channel, client2EncrytionChannel);
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
