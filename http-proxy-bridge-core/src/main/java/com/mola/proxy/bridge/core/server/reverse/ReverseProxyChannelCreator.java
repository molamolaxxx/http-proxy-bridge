package com.mola.proxy.bridge.core.server.reverse;

import com.mola.proxy.bridge.core.config.ReverseServerItemConfig;
import com.mola.proxy.bridge.core.entity.ReverseChannelHandle;
import com.mola.proxy.bridge.core.enums.ReverseTypeEnum;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.Socks5AuthExt;
import com.mola.proxy.bridge.core.ext.def.DefaultServerSslAuthExt;
import com.mola.proxy.bridge.core.handlers.access.EncryptionAuthDecoder;
import com.mola.proxy.bridge.core.handlers.access.EncryptionAuthInboundHandler;
import com.mola.proxy.bridge.core.handlers.connect.ReverseProxyChannelManageHandler;
import com.mola.proxy.bridge.core.handlers.http.HttpRequestHandler;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5CommandRequestInboundHandler;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5InitialRequestInboundHandler;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5PasswordAuthRequestInboundHandler;
import com.mola.proxy.bridge.core.handlers.ssl.SslServerHandler;
import com.mola.proxy.bridge.core.handlers.udp.UdpServerHandler;
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.utils.AssertUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class ReverseProxyChannelCreator {

    private static final int CORE_POOL_SIZE = 10;

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelCreator.class);

    private final String host;

    private final int port;

    private final ReverseTypeEnum type;

    private final ReverseProxyChannelManageHandler reverseProxyChannelManageHandler;

    private final ThreadPoolExecutor channelCreateExecutor;

    private final String serverId;

    public ReverseProxyChannelCreator(String serverId, ReverseServerItemConfig config) {
        this.serverId = serverId;
        this.host = config.getRemoteHost();
        this.port = config.getRemotePort();
        this.reverseProxyChannelManageHandler = new ReverseProxyChannelManageHandler();

        ReverseTypeEnum type = ReverseTypeEnum.valueOf(config.getType());
        AssertUtil.notNull(type, "reverse type required");
        if (type.requireEncryption()) {
            ExtManager.setSslAuthExt(new DefaultServerSslAuthExt());
        }
        this.type = type;
        this.channelCreateExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, Math.max(config.getChannelNum() / 5, CORE_POOL_SIZE),
                5000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1024),
                new ThreadFactory() {
                    private final AtomicInteger threadIndex = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, String.format("channel-create-thread-%d", this.threadIndex.incrementAndGet()));
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    public Future<Channel> createChannelAsync() {
        return channelCreateExecutor.submit(this::createChannel);
    }

    public Channel createChannel() {
        Bootstrap proxyClientBootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        // 选择对应的http handler
        HttpRequestHandler httpRequestHandler = new HttpRequestHandler();
        UdpServerHandler udpServerHandler = new UdpServerHandler();
        try {
            proxyClientBootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, false)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new IdleStateHandler(
                                    120, 120, 120));
                            ch.pipeline().addLast(reverseProxyChannelManageHandler);
                            if (type.requireEncryption()) {
                                ch.pipeline().addLast(SslServerHandler.create());
                                ch.pipeline().addLast(new EncryptionAuthDecoder());
                                ch.pipeline().addLast(new EncryptionAuthInboundHandler(serverId));
                            }

                            if (type.isHttpProxy()) {
                                ch.pipeline().addLast(httpRequestHandler);
                            } else if (type.isUdp()) {
                                ch.pipeline().addLast(udpServerHandler);
                            } else if (type.isSocks5Proxy()) {
                                //socks5响应最后一个encode
                                ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);

                                //处理socks5初始化请求
                                ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                                ch.pipeline().addLast(new Socks5InitialRequestInboundHandler());

                                // 处理认证请求
                                Socks5AuthExt socks5AuthExt = ExtManager.getSocks5AuthExt();
                                if (socks5AuthExt != null && socks5AuthExt.requireAuth()) {
                                    ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder());
                                    ch.pipeline().addLast(new Socks5PasswordAuthRequestInboundHandler());
                                }

                                //处理connection请求
                                ch.pipeline().addLast(new Socks5CommandRequestDecoder());
                                ch.pipeline().addLast(new Socks5CommandRequestInboundHandler(group));
                            }
                        }
                    });

            ChannelFuture future = proxyClientBootstrap.connect(host, port).sync();
            if (!future.isSuccess()) {
                throw new RuntimeException("ReverseProxyServer start failed!");
            }
            ReverseProxyConnectPool.instance()
                    .addReverseChannelHandle(future.channel(), new ReverseChannelHandle(
                            group, httpRequestHandler, udpServerHandler
                    ));
            return future.channel();
        } catch (Exception e) {
            log.error("ReverseProxyServer start failed!", e);
            group.shutdownGracefully();
            httpRequestHandler.shutdown();
            udpServerHandler.shutdown();
        }
        return null;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
