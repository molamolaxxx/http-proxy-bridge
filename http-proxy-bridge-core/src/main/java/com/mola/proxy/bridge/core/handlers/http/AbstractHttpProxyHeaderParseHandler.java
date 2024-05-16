package com.mola.proxy.bridge.core.handlers.http;

import com.mola.proxy.bridge.core.entity.ProxyHttpHeader;
import com.mola.proxy.bridge.core.utils.HeaderParser;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description:
 * @date : 2024-05-12 22:21
 **/
public abstract class AbstractHttpProxyHeaderParseHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpProxyHeaderParseHandler.class);

    /**
     * 是否已经解析过
     */
    private volatile ProxyHttpHeader proxyHttpHeader;

    /**
     * 第一次建立链接，需要缓存数据报，防止数据丢失
     */
    private final Map<Channel, ByteBuf> msgMap = new ConcurrentHashMap<>(32);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("BaseHttpProxyRouteHandler exceptionCaught, channel = {}", ctx.channel(), cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel channel = ctx.channel();
        if(proxyHttpHeader != null) {
            channelReadWithHeader(ctx, msg, proxyHttpHeader);
        } else {
            ByteBuf buffer = null;
            if(msgMap.containsKey(channel) && msgMap.get(channel)!=null) {
                buffer = msgMap.get(channel);
            }else {
                buffer = ctx.alloc().buffer(1024 * 2);
            }
            buffer.writeBytes((ByteBuf) msg);
            msgMap.put(channel, buffer);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (Objects.nonNull(proxyHttpHeader)) {
            return;
        }

        final Channel client2proxyChannel = ctx.channel();
        ByteBuf clientRequestBuf = msgMap.get(client2proxyChannel);
        if (Objects.isNull(clientRequestBuf)) {
            return;
        }

        byte[] clientRequestBytes = new byte[clientRequestBuf.readableBytes()];
        clientRequestBuf.getBytes(0, clientRequestBytes);
        // 释放存放header的内存
        msgMap.remove(client2proxyChannel);
        RemotingHelper.releaseBuf(clientRequestBuf);

        // 解析proxy头信息
        proxyHttpHeader = parseProxyHeader(new String(clientRequestBytes), client2proxyChannel);

        // 执行自定义逻辑
        channelReadCompleteWithHeader(ctx, proxyHttpHeader, clientRequestBytes);
    }

    /**
     * 解析完header后channelReadComplete回调
     * @param ctx
     * @param header
     */
    protected abstract void channelReadCompleteWithHeader(ChannelHandlerContext ctx, ProxyHttpHeader header,
                                                          byte[] clientRequestBytes);

    /**
     * 解析完header后channelRead回调
     * @param ctx
     * @param header
     */
    protected abstract void channelReadWithHeader(ChannelHandlerContext ctx, Object msg,
                                                  ProxyHttpHeader header);


    protected ProxyHttpHeader parseProxyHeader(String header, Channel client2proxyChannel) {
        return HeaderParser.parse(header);
    }

    public void shutdown() {
        for (Map.Entry<Channel, ByteBuf> entry : msgMap.entrySet()) {
            entry.getKey().close();
            RemotingHelper.releaseBuf(entry.getValue());
        }
        msgMap.clear();
    }
}
