package com.mola.forward;

import com.mola.common.HttpRequestHandler;
import com.mola.common.ReverseProxyChannelManageHandler;
import com.mola.pool.ReverseProxyConnectPool;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: 正向代理服务器
 * @date : 2023-09-30 00:41
 **/
public class ForwardProxyServer {


    private AtomicBoolean start = new AtomicBoolean(false);

    private static final Logger log = LoggerFactory.getLogger(ForwardProxyServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture forwardSeverChannelFuture;

    private ChannelFuture proxyRegisterChannelFuture;

    public synchronized void start(int port, int reversePort) {
        if (start.get()) {
            return;
        }
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(8);
            // 正向代理
            forwardSeverChannelFuture = startForwardProxyServer(port);
            // 反向代理注册
            proxyRegisterChannelFuture = startReverseProxyRegisterServer(reversePort);
            if (!forwardSeverChannelFuture.isSuccess() || !proxyRegisterChannelFuture.isSuccess()) {
                log.error("ForwardProxyServer start failed!");
                return;
            }
            log.info("ForwardProxyServer start success!");
            log.info("forwardProxyServer channel is " + forwardSeverChannelFuture.channel().toString());
            log.info("proxyRegisterChannel channel is " + proxyRegisterChannelFuture.channel().toString());
            start.compareAndSet(false, true);
            forwardSeverChannelFuture.channel().closeFuture().sync();
            proxyRegisterChannelFuture.channel().closeFuture().sync();
        }
        catch (Exception e) {
            log.error("ForwardProxyServer start failed!", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void shutdown() {
        if (!start.get()) {
            return;
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();

        forwardSeverChannelFuture.channel().close();
        forwardSeverChannelFuture = null;
        proxyRegisterChannelFuture.channel().close();
        proxyRegisterChannelFuture = null;

        ReverseProxyConnectPool.instance().shutdown();
        System.gc();
        log.info("ForwardProxyServer has been shutdown");
        start.compareAndSet(true, false);
    }

    private ChannelFuture startForwardProxyServer(int port) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        WhiteListAccessHandler whiteListAccessHandler = new WhiteListAccessHandler();
        ForwardProxyChannelManageHandler forwardProxyChannelManageHandler = new ForwardProxyChannelManageHandler();
        DataTransferHandler dataTransferHandler = new DataTransferHandler();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        HttpRequestHandler httpRequestHandler = new HttpRequestHandler();
                        ch.closeFuture().addListener((ChannelFutureListener) future -> {
                            httpRequestHandler.shutdown();
                        });
                        ch.pipeline().addLast(whiteListAccessHandler);
                        ch.pipeline().addLast(new IdleStateHandler(30, 30, 30));
                        ch.pipeline().addLast(forwardProxyChannelManageHandler);
                        ch.pipeline().addLast(dataTransferHandler);
                        ch.pipeline().addLast(new HttpRequestHandler());
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture future = serverBootstrap.bind(port);
        // 给future添加监听器，监听关心的事件
        future.addListener((ChannelFutureListener) future1 -> {
            if (future.isSuccess()) {
                log.info("listening port " + port + " success");
            } else {
                log.info("listening port" + port + " failed");
            }
        });
        return future;
    }

    private ChannelFuture startReverseProxyRegisterServer(int port) throws InterruptedException {
        ReverseProxyChannelManageHandler reverseProxyChannelManageHandler = new ReverseProxyChannelManageHandler();
        DataReceiveHandler dataReceiveHandler = new DataReceiveHandler();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(30, 30, 30));
                        ch.pipeline().addLast(reverseProxyChannelManageHandler);
                        ch.pipeline().addLast(dataReceiveHandler);
                    }
                });
        ChannelFuture future = serverBootstrap.bind(port);
        // 给future添加监听器，监听关心的事件
        future.addListener((ChannelFutureListener) f -> {
            if (future.isSuccess()) {
                log.info("listening port " + port + " success");
            } else {
                log.info("listening port" + port + " failed");
            }
        });
        return future;
    }
}
