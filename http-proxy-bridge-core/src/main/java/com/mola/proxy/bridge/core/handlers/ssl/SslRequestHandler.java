package com.mola.proxy.bridge.core.handlers.ssl;

import com.mola.proxy.bridge.core.entity.ConnectionRouteRule;
import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.handlers.http.AbstractHttpProxyHeaderParseHandler;
import com.mola.proxy.bridge.core.router.ConnectionRouter;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: ssl 数据转发
 * @date : 2023-09-30 09:10
 **/

public class SslRequestHandler extends AbstractHttpProxyHeaderParseHandler {

    private static final Logger log = LoggerFactory.getLogger(SslRequestHandler.class);

    private final Channel client2EncryptionChannel;

    private final Bootstrap encryption2ServerBootstrap;

    private final String defaultHost;

    private final int defaultPort;

    private Channel encryption2ServerChannel;

    public SslRequestHandler(Channel client2EncryptionChannel, Bootstrap encryption2ServerBootstrap,
                             String host, int port) {
        this.defaultHost = host;
        this.defaultPort = port;
        this.client2EncryptionChannel = client2EncryptionChannel;
        this.encryption2ServerBootstrap = encryption2ServerBootstrap;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("SslRequestHandler error, channel = {}", ctx.channel(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        encryption2ServerChannel.close();
    }

    @Override
    protected void channelReadCompleteWithHeader(ChannelHandlerContext ctx, ProxyHttpHeader header,
                                                 byte[] clientRequestBytes) {
        try {
            // 匹配域名规则
            ConnectionRouteRule routeRule = ConnectionRouter.instance().match(header.getHost());
            ChannelFuture future = null;
            if (routeRule != null) {
                future = encryption2ServerBootstrap.connect(routeRule.getRemoteHost(),
                        routeRule.getRemotePort()).sync();
                log.debug("hit host rule, host = {}, remote = {}", header.getHost(),
                        String.format("%s:%s", routeRule.getRemoteHost(), routeRule.getRemotePort()));
            } else {
                future = encryption2ServerBootstrap.connect(defaultHost, defaultPort).sync();
            }
            if (!future.isSuccess()) {
                throw new RuntimeException("encryption2ServerBootstrap connect failed!");
            }
            encryption2ServerChannel = future.channel();
            SslHandler sslHandler = encryption2ServerChannel.pipeline().get(SslHandler.class);
            // 握手完成回调
            sslHandler.handshakeFuture().addListener(future1 -> {
                // SslHandler后增加响应handler，|SslResponseHandler|SslHandler| -----> (forward)
                encryption2ServerChannel.pipeline().addLast(new SslResponseHandler(client2EncryptionChannel));

                // 发送header
                ByteBuf buffer = encryption2ServerChannel
                        .alloc()
                        .buffer(clientRequestBytes.length);
                buffer.writeBytes(clientRequestBytes);
                encryption2ServerChannel.writeAndFlush(buffer);
            });
        } catch (Exception e) {
            log.error("channelReadCompleteWithHeader exception, channel = {}", ctx.channel(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void channelReadWithHeader(ChannelHandlerContext ctx, Object msg, ProxyHttpHeader header) {
        encryption2ServerChannel.writeAndFlush(msg);
    }
}
