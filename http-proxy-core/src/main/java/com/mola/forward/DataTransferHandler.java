package com.mola.forward;

import com.mola.forward.group.ProxyGroup;
import com.mola.forward.group.ProxyGroupRegistry;
import com.mola.pool.ReverseProxyConnectPool;
import com.mola.reverse.ReverseProxyChannelMonitor;
import com.mola.utils.RemotingHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy
 * @Description:
 * @date : 2023-09-30 09:10
 **/

@ChannelHandler.Sharable
public class DataTransferHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(ReverseProxyChannelMonitor.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ReverseProxyConnectPool connectPool = ReverseProxyConnectPool.instance();
        ProxyGroupRegistry groupRegistry = ProxyGroupRegistry.instance();

        int port = RemotingHelper.fetchChannelLocalPort(ctx.channel());
        ProxyGroup proxyGroup = groupRegistry.fetchGroupByForwardPort(port);

        if (connectPool.getReverseProxyChannels(proxyGroup.getReversePort()).size() == 0) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel reverseChannel = connectPool.allocate(ctx.channel(), proxyGroup.getReversePort());
        if (Objects.isNull(reverseChannel)) {
            connectPool.clearChannels(proxyGroup.getReversePort());
            reverseChannel = connectPool.allocate(ctx.channel(), proxyGroup.getReversePort());
        }

        if (Objects.isNull(reverseChannel)) {
            log.warn("allocate reverseChannel failed, no available channel");
            ctx.fireChannelRead(msg);
            return;
        }

        reverseChannel.writeAndFlush(msg);
    }
}
