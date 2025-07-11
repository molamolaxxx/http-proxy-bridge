package com.mola.proxy.bridge.core.server.encryption;

import com.mola.proxy.bridge.core.config.EncryptionServerItemConfig;
import com.mola.proxy.bridge.core.enums.EncryptionTypeEnum;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.def.DefaultClientSslAuthExt;
import com.mola.proxy.bridge.core.handlers.ssl.SslClientHandler;
import com.mola.proxy.bridge.core.handlers.ssl.SslRequestHandler;
import com.mola.proxy.bridge.core.handlers.udp.UdpEncryptionHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: ssl加密机
 * @date : 2023-10-22 18:56
 **/
public class SslEncryptionProxyServer {

    private static final Logger log = LoggerFactory.getLogger(SslEncryptionProxyServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Bootstrap encryptionClientBootstrap;

    private String remoteHost;

    private int remotePort;

    private final AtomicBoolean start = new AtomicBoolean(false);

    public synchronized void start(EncryptionServerItemConfig itemConfig) {
        if (start.get()) {
            return;
        }
        try {
            if (ExtManager.getSslAuthExt() == null) {
                ExtManager.setSslAuthExt(new DefaultClientSslAuthExt());
            }

            this.bossGroup = new NioEventLoopGroup(1);
            this.workerGroup = new NioEventLoopGroup(8);
            this.remoteHost = itemConfig.getRemoteHost();
            this.remotePort = itemConfig.getRemotePort();

            EncryptionTypeEnum encryptionTypeEnum = EncryptionTypeEnum.valueOf(itemConfig.getType());

            // 连接到远程ssl加密服务器的netty客户端
            encryptionClientBootstrap = createEncryptionClient(encryptionTypeEnum);

            ChannelFuture channelFuture = null;
            if (EncryptionTypeEnum.TCP == encryptionTypeEnum) {
                // 浏览器直接连接的代理服务器
                channelFuture = startEncryptionProxyServer(itemConfig.getPort(),
                        itemConfig.getAppointProxyHeader());
            } else if (EncryptionTypeEnum.UDP == encryptionTypeEnum) {
                channelFuture = startEncryptionUdpServer(itemConfig.getPort(),
                        itemConfig.getAppointProxyHeader());
            }

            channelFuture.await();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("SslEncryptionProxyServer start failed!", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }


    /**
     * 加密机http代理server
     * @param port
     * @return
     */
    private ChannelFuture startEncryptionProxyServer(int port, String appointProxyHeader) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        SslRequestHandler sslRequestHandler = new SslRequestHandler(
                                ch,
                                encryptionClientBootstrap,
                                remoteHost,
                                remotePort,
                                appointProxyHeader);
                        pipeline.addLast(sslRequestHandler);
                        // 释放堆外内存
                        ch.closeFuture().addListener((ChannelFutureListener) future -> sslRequestHandler.shutdown());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

        ChannelFuture future = serverBootstrap.bind(port);
        // 给future添加监听器，监听关心的事件
        future.addListener((ChannelFutureListener) future1 -> {
            if (future.isSuccess()) {
                log.info("[TcpServer] listening port " + port + " success");
            } else {
                log.info("[TcpServer] listening port " + port + " failed");
            }
        });
        return future;
    }

    /**
     * 加密机udp代理server
     * @param port
     * @return
     */
    private ChannelFuture startEncryptionUdpServer(int port, String appointProxyHeader) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workerGroup)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.SO_RCVBUF, 2048 * 1024)
                .option(ChannelOption.SO_SNDBUF, 1024 * 1024)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    public void initChannel(NioDatagramChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        UdpEncryptionHandler udpEncryptionHandler = new UdpEncryptionHandler(
                                ch,
                                encryptionClientBootstrap,
                                remoteHost,
                                remotePort,
                                appointProxyHeader
                        );
                        pipeline.addLast(udpEncryptionHandler);
                        // 释放堆外内存
                        ch.closeFuture().addListener((ChannelFutureListener) future -> udpEncryptionHandler.shutdown());
                    }
                });

        ChannelFuture future = bootstrap.bind(port);
        // 给future添加监听器，监听关心的事件
        future.addListener((ChannelFutureListener) future1 -> {
            if (future.isSuccess()) {
                log.info("[UdpServer] listening port " + port + " success");
            } else {
                log.info("[UdpServer] listening port " + port + " failed");
            }
        });
        return future;
    }
    /**
     * 加密机client
     * @return
     */
    public Bootstrap createEncryptionClient(EncryptionTypeEnum encryptionTypeEnum) {
        Bootstrap encryptionClientBootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            encryptionClientBootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            if (EncryptionTypeEnum.TCP == encryptionTypeEnum) {
                                ch.pipeline().addLast(SslClientHandler.create());
                            }
                        }
                    });
            return encryptionClientBootstrap;
        } catch (Exception e) {
            log.error("EncryptionClient start failed!", e);
            group.shutdownGracefully();
        }
        return null;
    }
}
