package com.mola.proxy.bridge.core.handlers.http;

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.utils.HeaderParser;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: http处理器，负责连接站点&发送数据
 * @date : 2020-09-04 10:50
 **/
public class HttpRequestHandler extends AbstractHttpProxyHeaderParseHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpRequestHandler.class);

    private static final EventLoopGroup HTTP_REQUEST_GROUP = new NioEventLoopGroup(4);

    private static final String CONNECTION_ESTABLISHED_RESP = "HTTP/1.1 200 Connection Established\r\n\r\n";

    private final Map<Channel, Channel> channelMap = new ConcurrentHashMap<>(32);

    private final Bootstrap httpInvokeBootstrap = new Bootstrap();

    private final CountDownLatch waitForChannelConnect = new CountDownLatch(1);

    @SuppressWarnings("rawtypes")
    public HttpRequestHandler() {
        httpInvokeBootstrap.group(HTTP_REQUEST_GROUP).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpResponseHandler(channelMap));
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HttpRequestHandler exceptionCaught, channel = {}", ctx.channel(), cause);
    }

    @Override
    protected void channelReadWithHeader(ChannelHandlerContext ctx, Object msg, ProxyHttpHeader header) throws Exception{
        Channel proxy2ServerChannel = channelMap.get(ctx.channel());
        if (proxy2ServerChannel == null
                && waitForChannelConnect.await(5000, TimeUnit.MILLISECONDS)) {
            proxy2ServerChannel = channelMap.get(ctx.channel());
        }
        if (proxy2ServerChannel != null) {
            proxy2ServerChannel.writeAndFlush(msg);
        }
    }

    @Override
    protected void channelReadCompleteWithHeader(ChannelHandlerContext ctx, ProxyHttpHeader header,
                                                 byte[] clientRequestBytes) {
        // 内网穿透 映射
        transferHost(header);

        // 连接目标host，异步回调
        ChannelFuture future = httpInvokeBootstrap.connect(header.getHost(), header.getPort());
        future.addListener((ChannelFutureListener) completedFuture -> {
            final Channel client2proxyChannel = ctx.channel();
            if (!completedFuture.isSuccess()) {
                log.error("connect failing " + header.getTargetAddress());
                return;
            }

            Channel proxy2ServerChannel = completedFuture.channel();
            proxy2ServerChannel.closeFuture().addListener((ChannelFutureListener) chCloseFuture -> {
                if (chCloseFuture.isSuccess()) {
                    log.info("http channel close! close client channel, channel = " + client2proxyChannel);
                    client2proxyChannel.close();
                }
            });

            log.info("connect success！address = " + header.getTargetAddress());
            waitForChannelConnect.countDown();

            // 双端映射
            channelMap.put(client2proxyChannel, proxy2ServerChannel);
            channelMap.put(proxy2ServerChannel, client2proxyChannel);

            // connect方法，直接返回
            if(header.isConnectMethod()) {
                // 指定的header（非标准http代理头）不需要返回响应
                if (header.isAppoint()) {
                    return;
                }
                ByteBuf buffer = client2proxyChannel
                        .alloc()
                        .buffer(CONNECTION_ESTABLISHED_RESP.getBytes().length);
                buffer.writeBytes(CONNECTION_ESTABLISHED_RESP.getBytes());
                client2proxyChannel.writeAndFlush(buffer);
            } else {
                ByteBuf buffer = proxy2ServerChannel
                        .alloc()
                        .buffer(clientRequestBytes.length);
                buffer.writeBytes(clientRequestBytes);
                proxy2ServerChannel.writeAndFlush(buffer);
            }
        });
    }

    @Override
    public void shutdown() {
        super.shutdown();
        for (Map.Entry<Channel, Channel> entry : channelMap.entrySet()) {
            entry.getKey().close();
            entry.getValue().close();
        }
        channelMap.clear();
    }
}
