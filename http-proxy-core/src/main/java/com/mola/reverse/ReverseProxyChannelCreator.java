package com.mola.reverse;

import com.mola.common.HttpRequestHandler;
import com.mola.common.ReverseProxyChannelManageHandler;
import com.mola.enums.ServerTypeEnum;
import com.mola.forward.ForwardProxyServer;
import com.mola.pool.ReverseProxyConnectPool;
import com.mola.socks5.Socks5CommandRequestInboundHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class ReverseProxyChannelCreator {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelCreator.class);

    private String host;

    private int port;

    private ServerTypeEnum type;

    private ReverseProxyChannelManageHandler reverseProxyChannelManageHandler;

    public ReverseProxyChannelCreator(String host, int port, ServerTypeEnum type) {
        this.host = host;
        this.port = port;
        this.reverseProxyChannelManageHandler = new ReverseProxyChannelManageHandler();
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
                            ch.pipeline().addLast(new IdleStateHandler(60, 60, 60));
                            ch.pipeline().addLast(reverseProxyChannelManageHandler);
                            if (type == ServerTypeEnum.HTTP) {
                                ch.pipeline().addLast(httpRequestHandler);
                            } else if (type == ServerTypeEnum.SOCKS5) {

                                //socks5响应最后一个encode
                                ch.pipeline().addLast(Socks5ServerEncoder.DEFAULT);

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
}
