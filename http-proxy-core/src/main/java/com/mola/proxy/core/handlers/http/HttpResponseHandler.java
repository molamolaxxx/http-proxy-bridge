package com.mola.proxy.core.handlers.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: http响应，负责发送http响应数据到代理连接（正向 || 反向）
 * @date : 2020-09-04 10:50
 **/
public class HttpResponseHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(HttpResponseHandler.class);

    private final Map<Channel,Channel> map;

    public HttpResponseHandler(Map<Channel, Channel> map) {
        this.map = map;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("HttpResponseHandler exceptionCaught, will remove channel mapping, " +
                "channel = " + ctx.channel(), cause);
        map.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel proxy2ServerChannel = ctx.channel();
        Channel client2ProxyChannel = map.get(proxy2ServerChannel);
        if (client2ProxyChannel == null) {
            log.warn("client2ProxyChannel has been removed, proxy2ServerChannel = " + proxy2ServerChannel);
            return;
        }
        client2ProxyChannel.writeAndFlush(msg);
    }
}
