package com.mola.proxy.bridge.core.server.forward;

import com.mola.proxy.bridge.core.entity.ProxyBridge;
import com.mola.proxy.bridge.core.server.encryption.SslContextFactory;
import com.mola.proxy.bridge.core.enums.ServerTypeEnum;
import com.mola.proxy.bridge.core.handlers.access.WhiteListAccessHandler;
import com.mola.proxy.bridge.core.handlers.connect.ForwardProxyChannelManageHandler;
import com.mola.proxy.bridge.core.handlers.transfer.DataReceiveHandler;
import com.mola.proxy.bridge.core.registry.ProxyBridgeRegistry;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.def.DefaultServerSslAuthExt;
import com.mola.proxy.bridge.core.handlers.connect.ReverseProxyChannelManageHandler;
import com.mola.proxy.bridge.core.handlers.http.HttpRequestHandler;
import com.mola.proxy.bridge.core.handlers.transfer.DataTransferHandler;
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5CommandRequestInboundHandler;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5InitialRequestInboundHandler;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 正向代理服务器
 * @date : 2023-09-30 00:41
 **/
public class ForwardProxyServer {

    private final AtomicBoolean start = new AtomicBoolean(false);

    private static final Logger log = LoggerFactory.getLogger(ForwardProxyServer.class);

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private ChannelFuture forwardSeverChannelFuture;

    private ChannelFuture proxyRegisterChannelFuture;

    private static final Map<Integer, ChannelFuture> reverseChannelFutureCacheMap = new ConcurrentHashMap<>();

    /**
     * 启动代理服务器，包括正向代理，反向接收服务
     * @param port
     * @param reversePort
     * @param type
     */
    public synchronized void start(int port, int reversePort, ServerTypeEnum type) {
        if (start.get()) {
            return;
        }
        ProxyBridgeRegistry.instance().register(ProxyBridge.build(port, reversePort));
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup(8);
            // 正向代理服务启动
            forwardSeverChannelFuture = startForwardProxyServer(port, reversePort, type);

            // 反向代理接收服务启动
            proxyRegisterChannelFuture = startReverseProxyRegisterServer(reversePort);

            forwardSeverChannelFuture.await();
            proxyRegisterChannelFuture.await();
            if (!forwardSeverChannelFuture.isSuccess() || !proxyRegisterChannelFuture.isSuccess()) {
                log.error("ForwardProxyServer start failed!");
                return;
            }
            log.info("ForwardProxyServer start success!");
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

    private ChannelFuture startForwardProxyServer(int port, int reversePort, ServerTypeEnum type) {
        if (ServerTypeEnum.HTTP == type) {
            return startForwardHttpProxyServer(port, false, false);
        } else if (ServerTypeEnum.SOCKS5 == type) {
            return startForwardSocks5ProxyServer(port, false);
        } else if (ServerTypeEnum.SSL_HTTP == type) {
            return startForwardHttpProxyServer(port, true, false);
        } else if (ServerTypeEnum.SSL_SOCKS5 == type) {
            return startForwardSocks5ProxyServer(port, true);
        } else if (ServerTypeEnum.SSL_TRANSFER == type) {
            return startForwardHttpProxyServer(port, true, true);
        }
        throw new RuntimeException("unknown type, " + type);
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

    private ChannelFuture startForwardHttpProxyServer(int port, boolean useSsl, boolean pureTransfer) {
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

                        // 纯转发，不配置代理服务
                        if (pureTransfer) {
                            return;
                        }

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
                log.info("[http-forward] listening port " + port + " success");
            } else {
                log.info("[http-forward] listening port " + port + " failed");
            }
        });
        return future;
    }

    private ChannelFuture startForwardSocks5ProxyServer(int port, boolean useSsl) {
        if (useSsl && ExtManager.getSslAuthExt() == null) {
            ExtManager.setSslAuthExt(new DefaultServerSslAuthExt());
        }

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

                        // 白名单，ssl连接不需要白名单校验
                        if (useSsl) {
                            SslHandler sslHandler = SslContextFactory.createSslHandler(false);
                            ch.pipeline().addLast(sslHandler);
                        } else { // 客户端使用加密机不需要白名单验证
                            ch.pipeline().addLast(whiteListAccessHandler);
                        }

                        if (needTransfer(ch)) {
                            // 转发请求到反向代理
                            pipeline.addLast(dataTransferHandler);
                        } else {
                            //socks5响应最后一个encode
                            pipeline.addLast(Socks5ServerEncoder.DEFAULT);

                            //处理socks5初始化请求
                            pipeline.addLast(new Socks5InitialRequestDecoder());
                            pipeline.addLast(new Socks5InitialRequestInboundHandler());

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
        synchronized (ForwardProxyServer.class) {
            return startReverseProxyRegisterServerInner(port);
        }
    }


    private ChannelFuture startReverseProxyRegisterServerInner(int port) {
        // 缓存反向启动服务，有可能多个bridge关联相同reverse
        if (reverseChannelFutureCacheMap.containsKey(port)) {
            return reverseChannelFutureCacheMap.get(port);
        }
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
                log.info("[reverse-receiver] listening port " + port + " success");
            } else {
                log.info("[reverse-receiver] listening port " + port + " failed");
            }
        });
        reverseChannelFutureCacheMap.put(port, future);
        return future;
    }


    private boolean needTransfer(Channel ch) {
        ProxyBridgeRegistry bridgeRegistry = ProxyBridgeRegistry.instance();
        ProxyBridge proxyBridge = bridgeRegistry.fetchBridgeByForwardPort(RemotingHelper.fetchChannelLocalPort(ch));

        ReverseProxyConnectPool connectPool = ReverseProxyConnectPool.instance();
        if (connectPool.getReverseProxyChannels(proxyBridge.getReversePort()).size() == 0) {
            return false;
        }
        return true;
    }
}
