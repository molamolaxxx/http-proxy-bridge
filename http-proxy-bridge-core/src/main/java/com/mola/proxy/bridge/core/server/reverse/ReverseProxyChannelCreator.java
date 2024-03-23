package com.mola.proxy.bridge.core.server.reverse;

import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.def.DefaultServerSslAuthExt;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5CommandRequestInboundHandler;
import com.mola.proxy.bridge.core.handlers.socks5.Socks5InitialRequestInboundHandler;
import com.mola.proxy.bridge.core.entity.ReverseChannelHandle;
import com.mola.proxy.bridge.core.handlers.http.HttpRequestHandler;
import com.mola.proxy.bridge.core.handlers.connect.ReverseProxyChannelManageHandler;
import com.mola.proxy.bridge.core.enums.ReverseTypeEnum;
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.server.encryption.SslContextFactory;
import com.mola.proxy.bridge.core.utils.AssertUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class ReverseProxyChannelCreator {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelCreator.class);

    private String host;

    private int port;

    private ReverseTypeEnum type;

    private ReverseProxyChannelManageHandler reverseProxyChannelManageHandler;

    public ReverseProxyChannelCreator(String host, int port, ReverseTypeEnum type) {
        this.host = host;
        this.port = port;
        this.reverseProxyChannelManageHandler = new ReverseProxyChannelManageHandler();
        AssertUtil.notNull(type, "reverse type required");
        if (type.requireEncryption()) {
            ExtManager.setSslAuthExt(new DefaultServerSslAuthExt());
        }
        this.type = type;
    }

    public Channel createChannel() {
        Bootstrap proxyClientBootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        HttpRequestHandler httpRequestHandler = new HttpRequestHandler();
        try {
            proxyClientBootstrap.group(group).channel(NioSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ch.pipeline().addLast(new IdleStateHandler(
                                    60, 60, 60));
                            ch.pipeline().addLast(reverseProxyChannelManageHandler);
                            if (type.requireEncryption()) {
                                SslHandler sslHandler = SslContextFactory.createSslHandler(false);
                                ch.pipeline().addLast(sslHandler);
                            }

                            if (type.isHttpProxy()) {
                                ch.pipeline().addLast(httpRequestHandler);
                            } else if (type == ReverseTypeEnum.SOCKS5) {
                                //socks5响应最后一个encode
                                ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);

                                //处理socks5初始化请求
                                ch.pipeline().addLast(new Socks5InitialRequestDecoder());
                                ch.pipeline().addLast(new Socks5InitialRequestInboundHandler());

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
                            group, httpRequestHandler
                    ));
            return future.channel();
        } catch (Exception e) {
            log.error("ReverseProxyServer start failed!", e);
            group.shutdownGracefully();
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
