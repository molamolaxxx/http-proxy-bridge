package com.mola.socks5;

import com.mola.forward.DataTransferHandler;
import com.mola.pool.ReverseProxyConnectPool;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author kdyzm
 * @date 2021-04-23
 */
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
        log.debug("prepare connect 2 server，ip={},port={}", msg.dstAddr(), msg.dstPort());

        // 连接http服务器
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        //添加服务端写客户端的Handler
                        ch.pipeline().addLast(new HttpServer2ClientInboundHandler(ctx));
                    }
                });
        ChannelFuture future = bootstrap.connect(msg.dstAddr(), msg.dstPort());
        future.addListener((ChannelFutureListener) future1 -> {
            if (future1.isSuccess()) {
                log.info("socks5 connect 2 server success!,address={},port={}", msg.dstAddr(), msg.dstPort());
                //添加客户端转发请求到服务端的Handler
                ctx.pipeline().addLast(new Client2HttpServerInboundHandler(future1));


                DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, socks5AddressType);
                ctx.writeAndFlush(commandResponse);
                ctx.pipeline().remove(Socks5CommandRequestInboundHandler.class);
                ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
            } else {
                log.error("socks5 connect 2 server failed!,address={},port={}", msg.dstAddr(), msg.dstPort());
                DefaultSocks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, socks5AddressType);
                ctx.writeAndFlush(commandResponse);
                future1.channel().close();
            }
        });
    }

}
