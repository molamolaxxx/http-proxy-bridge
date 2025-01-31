package com.mola.proxy.bridge.core.handlers.udp;

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.entity.UdpPacket;
import com.mola.proxy.bridge.core.handlers.http.AbstractHttpProxyHeaderParseHandler;
import com.mola.proxy.bridge.core.utils.HeaderParser;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: http处理器，负责连接站点&发送数据
 * @date : 2020-09-04 10:50
 **/
public class UdpServerHandler extends AbstractHttpProxyHeaderParseHandler {

    private static final Logger log = LoggerFactory.getLogger(UdpServerHandler.class);

    private static final EventLoopGroup UDP_REQUEST_GROUP = new NioEventLoopGroup(4);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("UdpRequestHandler exceptionCaught, channel = {}", ctx.channel(), cause);
    }

    @Override
    public void channelReadWithHeader(ChannelHandlerContext ctx, Object msg, ProxyHttpHeader header) throws Exception{
    }

    @Override
    protected void channelReadCompleteWithHeader(ChannelHandlerContext ctx, ProxyHttpHeader header,
                                                 byte[] clientRequestBytes) {
        // 内网穿透 映射
        transferHost(header);

        Channel targetConnectChannel = null;
        // encoder|UdpResponseHandler| <- target
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap = clientBootstrap
                    .group(UDP_REQUEST_GROUP)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<NioDatagramChannel>() {
                        @Override
                        protected void initChannel(NioDatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new UdpServerResponseHandler(ctx.channel()));
                        }
                    });

            targetConnectChannel = clientBootstrap.bind(0).sync().channel();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // forward -> decoder|UdpServerRequestHandler -> target
        ChannelPipeline pipeline = ctx.pipeline();
        pipeline.addLast(new UdpPacketDecoder());
        pipeline.addLast(new UdpPacketEncoder());
        pipeline.addLast(new UdpServerRequestHandler(targetConnectChannel, header));
        pipeline.remove(UdpServerHandler.class);
    }

    @Override
    protected ProxyHttpHeader parseProxyHeader(String header, Channel client2proxyChannel) {
        return HeaderParser.parse(header);
    }

    public static class UdpServerRequestHandler extends SimpleChannelInboundHandler<UdpPacket> {

        private static final Logger log = LoggerFactory.getLogger(UdpServerResponseHandler.class);

        private final Channel targetConnectChannel;

        private final ProxyHttpHeader header;

        public UdpServerRequestHandler(Channel targetConnectChannel, ProxyHttpHeader header) {
            this.targetConnectChannel = targetConnectChannel;
            this.header = header;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("UdpResponseHandler exceptionCaught, will remove channel mapping, " +
                    "channel = " + ctx.channel(), cause);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, UdpPacket msg) throws Exception {
            log.debug("[UdpRequestHandler] 接收到消息，开始发送到target：" + msg.getBodyStr(), "channel = "+ ctx.channel());
            DatagramPacket datagramPacket = msg.buildDatagramPacket(ctx,
                    new InetSocketAddress(header.getHost(), header.getPort()));
            targetConnectChannel.writeAndFlush(datagramPacket);
        }
    }

    public static class UdpServerResponseHandler extends SimpleChannelInboundHandler<DatagramPacket> {

        private static final Logger log = LoggerFactory.getLogger(UdpServerResponseHandler.class);

        private final Channel client2ProxyChannel;

        public UdpServerResponseHandler(Channel client2ProxyChannel) {
            this.client2ProxyChannel = client2ProxyChannel;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("UdpResponseHandler exceptionCaught, will remove channel mapping, " +
                    "channel = " + ctx.channel(), cause);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
            Channel proxy2ServerChannel = ctx.channel();
            if (client2ProxyChannel == null) {
                log.warn("client2ProxyChannel has been removed, proxy2ServerChannel = " + proxy2ServerChannel);
                RemotingHelper.releaseBuf(msg);
                return;
            }
            UdpPacket udpPacket = UdpPacket.buildFrom(msg);
            log.debug("[UdpResponseHandler] 接收到target消息，开始返回：" + udpPacket.getBodyStr());
            client2ProxyChannel.writeAndFlush(udpPacket);
        }
    }
}
