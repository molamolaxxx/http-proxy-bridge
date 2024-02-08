package com.mola.proxy.bridge.core.handlers.http;

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.ext.ExtManager;
import com.mola.proxy.bridge.core.ext.HostMappingExt;
import com.mola.proxy.bridge.core.utils.HeaderParser;
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

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: http处理器，负责连接站点&发送数据
 * @date : 2020-09-04 10:50
 **/
public class HttpRequestHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestHandler.class);

    private final Map<Channel, Channel> channelMap = new HashMap<>();

    /**
     * 第一次建立链接，需要缓存数据报，防止数据丢失
     */
    private final Map<Channel, ByteBuf> msgMap = new HashMap<>();

    private Bootstrap httpInvokeBootstrap = new Bootstrap();

    private static EventLoopGroup group = new NioEventLoopGroup(4);

    private final String CONNECTION_ESTABLISHED_RESP = "HTTP/1.1 200 Connection Established\r\n\r\n";


    @SuppressWarnings("rawtypes")
    public HttpRequestHandler() {
        httpInvokeBootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(new HttpResponseHandler(channelMap));
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

        // 内网穿透 映射
        transferHost(header);

        ChannelFuture future = httpInvokeBootstrap.connect(header.getHost(), header.getPort()).sync();
        if (!future.isSuccess()) {
            log.error("connect failing " + header.getTargetAddress());
        }
        proxy2ServerChannel = future.channel();
        proxy2ServerChannel.closeFuture().addListener((ChannelFutureListener) future1 -> {
            if (future1.isSuccess()) {
                log.info("http channel close! close client channel ");
                client2proxyChannel.close();
            }
        });

        log.info("connect success！address = " + header.getTargetAddress());

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
        for (Map.Entry<Channel, ByteBuf> entry : msgMap.entrySet()) {
            entry.getKey().close();
            entry.getValue().release();
        }
        channelMap.clear();
        msgMap.clear();
    }

    private void transferHost(ProxyHttpHeader header) {
        HostMappingExt hostMappingExt = ExtManager.getHostMappingExt();
        if (hostMappingExt == null) {
            return;
        }
        String mappedAddress = hostMappingExt.fetchMappedAddress(header.getHost(), header.getPort());
        if (mappedAddress == null) {
            return;
        }
        String[] hostAndPort = mappedAddress.split(":");
        if (hostAndPort.length == 0) {
            return;
        }
        if (hostAndPort.length == 1) {
            header.setHost(hostAndPort[0]);
        }
        if (hostAndPort.length == 2) {
            header.setHost(hostAndPort[0]);
            header.setPort(Integer.valueOf(hostAndPort[1]));
        }
    }
}
