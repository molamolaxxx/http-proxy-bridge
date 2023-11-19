package com.mola.forward;

import com.mola.common.HttpRequestHandler;
import com.mola.common.ReverseProxyChannelManageHandler;
import com.mola.enums.ServerTypeEnum;
import com.mola.ext.ExtManager;
import com.mola.ext.def.DefaultClientSslAuthExt;
import com.mola.ext.def.DefaultServerSslAuthExt;
import com.mola.forward.group.ProxyGroup;
import com.mola.forward.group.ProxyGroupRegistry;
import com.mola.pool.ReverseProxyConnectPool;
import com.mola.socks5.Socks5CommandRequestInboundHandler;
import com.mola.socks5.Socks5InitialRequestInboundHandler;
import com.mola.ssl.SslContextFactory;
import com.mola.utils.RemotingHelper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.ssl.SslHandler;
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

    public synchronized void start(int port, int reversePort, ServerTypeEnum type) {
        if (start.get()) {
            return;
        }
        ProxyGroupRegistry.instance().register(new ProxyGroup(port, reversePort));
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(8);
            // 正向代理
            if (ServerTypeEnum.HTTP == type) {
                forwardSeverChannelFuture = startForwardProxyServer(port, false);
            } else if (ServerTypeEnum.SOCKS5 == type) {
                forwardSeverChannelFuture = startForwardSocks5ProxyServer(port);
            } else if (ServerTypeEnum.SSL == type) {
                forwardSeverChannelFuture = startForwardProxyServer(port, true);
            } else {
                throw new RuntimeException("unknown type, " + type);
            }

            // 反向代理注册
            proxyRegisterChannelFuture = startReverseProxyRegisterServer(reversePort);

            forwardSeverChannelFuture.await();
            proxyRegisterChannelFuture.await();
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

    private ChannelFuture startForwardProxyServer(int port, boolean useSsl) throws InterruptedException {
        if (useSsl && ExtManager.getSslAuthExt() == null) {
            ExtManager.setSslAuthExt(new DefaultServerSslAuthExt());
        }

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        WhiteListAccessHandler whiteListAccessHandler = new WhiteListAccessHandler();
        ForwardProxyChannelManageHandler forwardProxyChannelManageHandler = new ForwardProxyChannelManageHandler();
        DataTransferHandler dataTransferHandler = new DataTransferHandler();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        if (useSsl) {
                            SslHandler sslHandler = SslContextFactory.createSslHandler(false);
                            ch.pipeline().addLast(sslHandler);
                        } else { // 客户端使用加密机不需要白名单验证
                            ch.pipeline().addLast(whiteListAccessHandler);
                        }
                        ch.pipeline().addLast(new IdleStateHandler(30, 30, 30));
                        ch.pipeline().addLast(forwardProxyChannelManageHandler);
                        ch.pipeline().addLast(dataTransferHandler);

                        HttpRequestHandler httpRequestHandler = new HttpRequestHandler();
                        ch.closeFuture().addListener((ChannelFutureListener) future -> {
                            httpRequestHandler.shutdown();
                        });
                        ch.pipeline().addLast(httpRequestHandler);
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

    private ChannelFuture startForwardSocks5ProxyServer(int port) {
        EventLoopGroup clientWorkGroup = new NioEventLoopGroup();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ServerBootstrap bootstrap = new ServerBootstrap();

        // handler
        WhiteListAccessHandler whiteListAccessHandler = new WhiteListAccessHandler();
        DataTransferHandler dataTransferHandler = new DataTransferHandler();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 256)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        // 白名单
                        pipeline.addLast(whiteListAccessHandler);

                        //socks5响应最后一个encode
                        pipeline.addLast(Socks5ServerEncoder.DEFAULT);

                        //处理socks5初始化请求
                        pipeline.addLast(new Socks5InitialRequestDecoder());
                        pipeline.addLast(new Socks5InitialRequestInboundHandler());

                        if (needTransfer(ch)) {
                            // 转发请求到反向代理
                            pipeline.addLast(dataTransferHandler);
                        } else {
                            //处理connection请求
                            pipeline.addLast(new Socks5CommandRequestDecoder());
                            pipeline.addLast(new Socks5CommandRequestInboundHandler(clientWorkGroup));
                        }
                    }
                });
        ChannelFuture future = bootstrap.bind(port);
        log.info("socks5 netty server has started on port {}", port);
        return future;
    }


    private ChannelFuture startReverseProxyRegisterServer(int port) {
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
                        ch.pipeline().addLast(new IdleStateHandler(
                                30, 30, 30));
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


    private boolean needTransfer(Channel ch) {
        ProxyGroupRegistry groupRegistry = ProxyGroupRegistry.instance();
        ProxyGroup proxyGroup = groupRegistry.fetchGroupByForwardPort(RemotingHelper.fetchChannelLocalPort(ch));

        ReverseProxyConnectPool connectPool = ReverseProxyConnectPool.instance();
        if (connectPool.getReverseProxyChannels(proxyGroup.getReversePort()).size() == 0) {
            return false;
        }
        return true;
    }
}
