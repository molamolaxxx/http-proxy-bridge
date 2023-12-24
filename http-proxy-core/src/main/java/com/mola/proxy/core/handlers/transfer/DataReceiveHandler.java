package com.mola.proxy.core.handlers.transfer;

import com.mola.proxy.core.pool.ReverseProxyConnectPool;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description: 接收反向代理流量发送到正向代理的channel
 * @date : 2023-09-30 09:10
 **/

@ChannelHandler.Sharable
public class DataReceiveHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DataReceiveHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ReverseProxyConnectPool connectPool = ReverseProxyConnectPool.instance();
        Channel forwardChannel = connectPool.fetchForward(ctx.channel());
        if (Objects.isNull(forwardChannel)) {
            ctx.fireChannelRead(msg);
            log.warn("[DataReceiveHandler]can not find forward by reverse! " + ctx.channel());
            return;
        }

        forwardChannel.writeAndFlush(msg);
    }
}
