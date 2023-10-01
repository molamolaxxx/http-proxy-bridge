package com.mola.common;

import com.mola.forward.ForwardProxyChannelManageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HttpRequestHandler extends ChannelInboundHandlerAdapter {


    private static final Logger log = LoggerFactory.getLogger(HttpRequestHandler.class);

    private final Map<Channel,Channel> channelMap = new HashMap<>();

    /**
     * 第一次建立链接，需要缓存数据报，防止数据丢失
     */
    private final Map<Channel, ByteBuf> msgMap = new HashMap<>();

    private final Bootstrap httpInvokeBootstrap = new Bootstrap();

    private final EventLoopGroup group;

    private final String CONNECTION_ESTABLISHED_RESP = "HTTP/1.1 200 Connection Established\r\n\r\n";

    @SuppressWarnings("rawtypes")
    public HttpRequestHandler() {
        group = new NioEventLoopGroup();
        httpInvokeBootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast("toServer handler", new HttpResponseHandler(channelMap));
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        channelMap.remove(ctx.channel());
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if(channelMap.containsKey(channel) && channelMap.get(channel) != null) {
            Channel proxy2ServerChannel = channelMap.get(channel);
            proxy2ServerChannel.writeAndFlush(msg);
        } else {
            ByteBuf buffer = null;
            if(msgMap.containsKey(channel) && msgMap.get(channel)!=null) {
                buffer = msgMap.get(channel);
            }else {
                buffer = ctx.alloc().buffer(1024 * 2);
            }
            buffer.writeBytes((ByteBuf) msg);
            buffer.retain();
            msgMap.put(channel, buffer);
        }
    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        final Channel client2proxyChannel = ctx.channel();
        Channel proxy2ServerChannel = channelMap.get(client2proxyChannel);
        if (Objects.nonNull(proxy2ServerChannel)) {
            return;
        }

        ByteBuf clientRequestBuf = msgMap.get(client2proxyChannel);
        if (Objects.isNull(clientRequestBuf)) {
            return;
        }

        byte[] clientRequestBytes = new byte[clientRequestBuf.readableBytes()];
        clientRequestBuf.getBytes(0, clientRequestBytes);

        ProxyHttpHeader header = HeaderParser.parse(new String(clientRequestBytes));
        ChannelFuture future = httpInvokeBootstrap.connect(header.getHost(), header.getPort()).sync();
        if (!future.isSuccess()) {
            log.error("=========connect failing");
        }
        proxy2ServerChannel = future.channel();

        log.info("连接成功！" + proxy2ServerChannel + "，host = " + header.getHost());

        // 双端映射
        channelMap.put(client2proxyChannel, proxy2ServerChannel);
        channelMap.put(proxy2ServerChannel, client2proxyChannel);

        // connect方法，直接返回
        if(header.isConnectMethod()) {
            ByteBuf buffer = client2proxyChannel.alloc()
                    .buffer(CONNECTION_ESTABLISHED_RESP.getBytes().length);
            buffer.writeBytes(CONNECTION_ESTABLISHED_RESP.getBytes());
            client2proxyChannel.writeAndFlush(buffer);
            clientRequestBuf.release();
        } else {
            proxy2ServerChannel.writeAndFlush(msgMap.get(client2proxyChannel));
            clientRequestBuf.release();
        }
    }

    public void shutdown() {
        for (Map.Entry<Channel, Channel> entry : channelMap.entrySet()) {
            entry.getKey().close();
            entry.getValue().close();
        }
        channelMap.clear();
        msgMap.clear();
        group.shutdownGracefully();
    }
}
