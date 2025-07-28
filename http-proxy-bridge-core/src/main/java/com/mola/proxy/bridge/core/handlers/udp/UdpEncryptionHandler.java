package com.mola.proxy.bridge.core.handlers.udp;

import com.mola.proxy.bridge.core.config.EncryptionServerItemConfig;
import com.mola.proxy.bridge.core.entity.ConnectionRouteRule;
import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.entity.UdpPacket;
import com.mola.proxy.bridge.core.router.ConnectionRouter;
import com.mola.proxy.bridge.core.schedule.EventScheduler;
import com.mola.proxy.bridge.core.utils.HeaderParser;
import com.mola.proxy.bridge.core.utils.LoadBalanceSelector;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2025-01-27 13:15
 **/
public class UdpEncryptionHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger log = LoggerFactory.getLogger(UdpEncryptionHandler.class);

    private static final AtomicInteger EVENT_IDX = new AtomicInteger();

    protected final Channel client2EncryptionChannel;

    private final Bootstrap encryption2ServerBootstrap;

    private final String defaultHost;

    private final int defaultPort;

    private final ProxyHttpHeader proxyHttpHeader;

    private Channel encryption2ServerChannel;

    private UdpEncryptionResponseHandler udpEncryptionResponseHandler;

    public UdpEncryptionHandler(Channel client2EncryptionChannel, Bootstrap encryption2ServerBootstrap,
                                String host, int port, EncryptionServerItemConfig itemConfig) {
        if (itemConfig == null || itemConfig.getAppointProxyHeader() == null) {
            throw new RuntimeException("appointProxyHeader can not be null in udp mode!");
        }
        this.defaultHost = host;
        this.defaultPort = port;
        this.client2EncryptionChannel = client2EncryptionChannel;
        this.encryption2ServerBootstrap = encryption2ServerBootstrap;
        this.proxyHttpHeader = HeaderParser.parse(itemConfig.getAppointProxyHeader());

        // 指定头，在udp场景下用于确认目标ip和端口
        String appointProxyHeader = itemConfig.getAppointProxyHeader();

        EventScheduler.addEvent("prepareTcpConnect" + EVENT_IDX.incrementAndGet()
                , 1, () -> {
                    if (encryption2ServerChannel == null
                            || !encryption2ServerChannel.isOpen() || !encryption2ServerChannel.isActive()) {
                        log.info("[UdpEncryptionHandler] redo prepareTcpConnect");
                        prepareTcpConnect(proxyHttpHeader,
                                String.format("%s\r", appointProxyHeader).getBytes(StandardCharsets.UTF_8));
                    }
                });

        prepareTcpConnect(proxyHttpHeader,
                    String.format("%s\r", appointProxyHeader).getBytes(StandardCharsets.UTF_8));
    }

    protected synchronized void prepareTcpConnect(ProxyHttpHeader header, byte[] clientRequestBytes) {
        try {
            // 匹配域名规则
            ConnectionRouteRule routeRule = ConnectionRouter.instance().match(header.getHost());
            ChannelFuture future = null;
            if (routeRule != null) {
                RemotingHelper.HostAndPort hp = new RemotingHelper.HostAndPort(
                        LoadBalanceSelector.instance().select(routeRule.getRemoteAddressList())
                );
                future = encryption2ServerBootstrap.connect(hp.host, hp.port).sync();
                log.debug("hit host rule, host = {}, remote = {}",
                        header.getHost(), String.format("%s:%s", hp.host, hp.port));
            } else {
                future = encryption2ServerBootstrap.connect(defaultHost, defaultPort).sync();
            }
            if (!future.isSuccess()) {
                throw new RuntimeException("encryption2ServerBootstrap connect failed!");
            }
            encryption2ServerChannel = future.channel();

            // 发送header，建立通道
            ByteBuf buffer = encryption2ServerChannel
                    .alloc()
                    .buffer(clientRequestBytes.length);
            buffer.writeBytes(clientRequestBytes);
            encryption2ServerChannel.writeAndFlush(buffer).sync();

            // 增加encoder、decoder、responseHandler
            // |UdpSslResponseHandler|encoder|decoder| -----> (forward)
            ChannelPipeline pipeline = encryption2ServerChannel.pipeline();
            pipeline.addLast(new UdpPacketEncoder());
            pipeline.addLast(new UdpPacketDecoder());
            this.udpEncryptionResponseHandler = new UdpEncryptionResponseHandler(client2EncryptionChannel);
            pipeline.addLast(udpEncryptionResponseHandler);

            // 与forward的连接断开后(forward主动断开)，同步断开与client的连接，并重新建立连接
            encryption2ServerChannel.closeFuture()
                    .addListener((ChannelFutureListener) closeFuture -> {
                        log.info("[UdpEncryptionHandler] channel closed");
                    });
        } catch (Exception e) {
            log.error("channelReadCompleteWithHeader exception", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
        udpEncryptionResponseHandler.setSender(packet.sender());
        UdpPacket udpPacket = UdpPacket.buildFrom(packet);
        if (encryption2ServerChannel != null) {
            encryption2ServerChannel.writeAndFlush(udpPacket);
        }
    }

    public void shutdown() {
        log.info("[UdpEncryptionHandler] shutdown, client2EncryptionChannel = {}", client2EncryptionChannel);
        if (encryption2ServerChannel != null && encryption2ServerChannel.isOpen()) {
            RemotingHelper.closeChannel(encryption2ServerChannel);
        }
        if (client2EncryptionChannel != null && client2EncryptionChannel.isOpen()) {
            RemotingHelper.closeChannel(client2EncryptionChannel);
        }
    }

    /**
     * udp响应解析
     */
    public static class UdpEncryptionResponseHandler extends SimpleChannelInboundHandler<UdpPacket>  {

        private static final Logger log = LoggerFactory.getLogger(UdpEncryptionResponseHandler.class);

        private final Channel client2EncryptionChannel;

        private InetSocketAddress sender;

        public UdpEncryptionResponseHandler(Channel client2EncryptionChannel) {
            this.client2EncryptionChannel = client2EncryptionChannel;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("UdpEncryptionResponseHandler error, channel = {}", ctx.channel(), cause);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, UdpPacket msg) throws Exception {
            client2EncryptionChannel.writeAndFlush(msg.buildDatagramPacket(ctx, sender));
        }

        public void setSender(InetSocketAddress sender) {
            this.sender = sender;
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.error("UdpEncryptionResponseHandler channelInactive, channel = {}", ctx.channel());
        }
    }
}
