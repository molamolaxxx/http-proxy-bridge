package com.mola.proxy.bridge.core.handlers.socks5;

import com.mola.proxy.bridge.core.ext.HostMappingExt;
import com.mola.proxy.bridge.core.ext.ExtManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2023-09-30 11:42
 **/
public class Socks5CommandRequestInboundHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private static final Logger log = LoggerFactory.getLogger(Socks5CommandRequestInboundHandler.class);

    private EventLoopGroup eventExecutors;

    public Socks5CommandRequestInboundHandler(EventLoopGroup eventExecutors) {
        this.eventExecutors = eventExecutors;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5CommandRequest msg) throws Exception {
        Socks5AddressType socks5AddressType = msg.dstAddrType();
        if (!msg.type().equals(Socks5CommandType.CONNECT)) {
            log.debug("receive commandRequest type={}", msg.type());
            ReferenceCountUtil.retain(msg);
            ctx.fireChannelRead(msg);
            return;
        }
        String host = msg.dstAddr();
        int port = msg.dstPort();
        HostMappingExt hostMappingExt = ExtManager.getHostMappingExt();
        String mappedAddress = null;
        if (hostMappingExt != null &&
                (mappedAddress = hostMappingExt.fetchMappedAddress(host, port)) != null) {
            String[] hostAndPort = mappedAddress.split(":");
            if (hostAndPort.length == 0) {
                return;
            }
            if (hostAndPort.length == 1) {
                host = hostAndPort[0];
            }
            if (hostAndPort.length == 2) {
                host = hostAndPort[0];
                port = Integer.parseInt(hostAndPort[1]);
            }
        }
        log.debug("prepare connect 2 server，ip={},port={}", host, port);

        // 代理服务器连接目标http站点服务器
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 站点响应流量，转发到代理连接中
                        ch.pipeline().addLast(new HttpServer2ClientInboundHandler(ctx));
                    }
                });
        ChannelFuture channelFuture = bootstrap.connect(host, port);
        final String hostFinal = host;
        final Integer portFinal = port;

        channelFuture.addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("socks5 connect 2 server success!,address={},port={}", hostFinal, portFinal);
                // 代理与http站点连接建立完成，后续通过Client2HttpServerInboundHandler转发请求流量
                ctx.pipeline().addLast(new Client2HttpServerInboundHandler(future));

                DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                ctx.writeAndFlush(commandResponse);
                ctx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
                ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
            } else {
                log.error("socks5 connect 2 server failed!,address={},port={}", msg.dstAddr(), msg.dstPort());
                DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
                ctx.writeAndFlush(commandResponse);
                future.channel().close();
            }
        });
    }

}
