package com.mola.common;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;

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
