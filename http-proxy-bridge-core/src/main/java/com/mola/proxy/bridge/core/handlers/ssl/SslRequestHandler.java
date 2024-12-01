package com.mola.proxy.bridge.core.handlers.ssl;

import com.mola.proxy.bridge.core.entity.ConnectionRouteRule;
import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.handlers.http.AbstractHttpProxyHeaderParseHandler;
import com.mola.proxy.bridge.core.router.ConnectionRouter;
import com.mola.proxy.bridge.core.utils.HeaderParser;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

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
                             String host, int port, String appointProxyHeader) {
        this.defaultHost = host;
        this.defaultPort = port;
        this.client2EncryptionChannel = client2EncryptionChannel;
        this.encryption2ServerBootstrap = encryption2ServerBootstrap;
        if (appointProxyHeader != null) {
            this.proxyHttpHeader = HeaderParser.parse(appointProxyHeader);
            channelReadCompleteWithHeader(null, proxyHttpHeader,
                    String.format("%s\r", appointProxyHeader).getBytes(StandardCharsets.UTF_8));
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("SslRequestHandler error, channel = {}", ctx.channel(), cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (encryption2ServerChannel != null) {
            encryption2ServerChannel.close();
        }
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
            Future<Channel> sslHandShakeFuture = sslHandler.handshakeFuture().addListener(handShakeFuture -> {
                // SslHandler后增加响应handler，|SslResponseHandler|SslHandler| -----> (forward)
                encryption2ServerChannel.pipeline().addLast(new SslResponseHandler(client2EncryptionChannel));

                // 发送header
                ByteBuf buffer = encryption2ServerChannel
                        .alloc()
                        .buffer(clientRequestBytes.length);
                buffer.writeBytes(clientRequestBytes);
                encryption2ServerChannel.writeAndFlush(buffer);
            });
            // 与forward的连接断开后(forward主动断开)，同步断开与client的连接，并回收资源
            encryption2ServerChannel.closeFuture()
                    .addListener((ChannelFutureListener) closeFuture -> shutdown());

            // 指定头场景下，如果不同步等待ssl握手，可能存在握手完成前就接受client发送的数据，而这部分数据无法被处理
            if (header.isAppoint()) {
                sslHandShakeFuture.sync();
            }
        } catch (Exception e) {
            if (ctx != null) {
                log.error("channelReadCompleteWithHeader exception, channel = {}", ctx.channel(), e);
            } else {
                log.error("channelReadCompleteWithHeader exception", e);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void channelReadWithHeader(ChannelHandlerContext ctx, Object msg, ProxyHttpHeader header) {
        if (encryption2ServerChannel != null) {
            encryption2ServerChannel.writeAndFlush(msg);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        if (encryption2ServerChannel != null && encryption2ServerChannel.isOpen()) {
            RemotingHelper.closeChannel(encryption2ServerChannel);
        }
        if (client2EncryptionChannel != null && client2EncryptionChannel.isOpen()) {
            RemotingHelper.closeChannel(client2EncryptionChannel);
        }
    }
}
