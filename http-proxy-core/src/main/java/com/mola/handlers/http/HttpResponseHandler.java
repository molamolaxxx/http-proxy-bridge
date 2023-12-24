package com.mola.handlers.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: http响应，负责发送http响应数据到代理连接（正向 || 反向）
 * @date : 2020-09-04 10:50
 **/
public class HttpResponseHandler extends ChannelInboundHandlerAdapter {

    private final Map<Channel,Channel> map;

    public HttpResponseHandler(Map<Channel, Channel> map) {
        this.map = map;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        map.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel proxy2ServerChannel = ctx.channel();
        Channel client2ProxyChannel = map.get(proxy2ServerChannel);
        client2ProxyChannel.writeAndFlush(msg);
    }
}
