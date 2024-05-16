package com.mola.proxy.bridge.core.server.encryption;

import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.def.DefaultClientSslAuthExt;
import com.mola.proxy.bridge.core.handlers.ssl.SslClientHandler;
import com.mola.proxy.bridge.core.handlers.ssl.SslRequestHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
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

    public synchronized void start(int port, String remoteHost, int remotePort) {
        if (start.get()) {
            return;
        }
        try {
            if (ExtManager.getSslAuthExt() == null) {
                ExtManager.setSslAuthExt(new DefaultClientSslAuthExt());
            }

            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(8);

            // 连接到远程ssl加密服务器的netty客户端
            encryptionClientBootstrap = createEncryptionClient();
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;

            // 浏览器直接连接的代理服务器
            ChannelFuture channelFuture = startEncryptionProxyServer(port);
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
     * @throws InterruptedException
     */
    private ChannelFuture startEncryptionProxyServer(int port) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        SslRequestHandler sslRequestHandler = new SslRequestHandler(
                                ch,
                                encryptionClientBootstrap,
                                remoteHost,
                                remotePort);
                        pipeline.addLast(sslRequestHandler);
                        // 释放堆外内存
                        ch.closeFuture().addListener((ChannelFutureListener) future -> {
                            sslRequestHandler.shutdown();
                        });
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture future = serverBootstrap.bind(port);
        // 给future添加监听器，监听关心的事件
        future.addListener((ChannelFutureListener) future1 -> {
            if (future.isSuccess()) {
                log.info("listening port " + port + " success");
            } else {
                log.info("listening port " + port + " failed");
            }
        });
        return future;
    }

    /**
     * 加密机client
     * @return
     */
    public Bootstrap createEncryptionClient() {
        Bootstrap encryptionClientBootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            encryptionClientBootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(SslClientHandler.create());
                        }
                    });
            return encryptionClientBootstrap;
        } catch (Exception e) {
            log.error("ReverseProxyServer start failed!", e);
            group.shutdownGracefully();
        }
        return null;
    }
}
