package com.mola.forward;

import com.mola.common.HttpRequestHandler;
import com.mola.common.ReverseProxyChannelManageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: 正向代理服务器
 * @date : 2023-09-30 00:41
 **/
public class ForwardProxyServer {


    private static final Logger log = LoggerFactory.getLogger(ForwardProxyServer.class);

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);

    private EventLoopGroup workerGroup = new NioEventLoopGroup(8);

    public void start(int port, int reversePort) {
        try {
//            // 正向代理
            ChannelFuture channelFuture = startForwardProxyServer(port);
            // 反向代理注册
            ChannelFuture proxyRegisterChannel = startReverseProxyRegisterServer(reversePort);
            if (!channelFuture.isSuccess() || !proxyRegisterChannel.isSuccess()) {
                log.error("ForwardProxyServer start failed!");
                return;
            }
            log.info("ForwardProxyServer start success!");
            log.info("forwardProxyServer channel is " + channelFuture.channel().toString());
            log.info("proxyRegisterChannel channel is " + proxyRegisterChannel.channel().toString());

            channelFuture.await();
            channelFuture.channel().closeFuture().sync();
            proxyRegisterChannel.channel().closeFuture().sync();
        }
        catch (Exception e) {
            log.error("ForwardProxyServer start failed!", e);
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private ChannelFuture startForwardProxyServer(int port) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(new WhiteListAccessHandler());
                        ch.pipeline().addLast(new ForwardProxyChannelManageHandler());
                        ch.pipeline().addLast(new DataTransferHandler());
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
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch)
                            throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(10, 10, 10));
                        ch.pipeline().addLast(new ReverseProxyChannelManageHandler());
                        ch.pipeline().addLast(new DataReceiveHandler());
                    }
                });
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
}
