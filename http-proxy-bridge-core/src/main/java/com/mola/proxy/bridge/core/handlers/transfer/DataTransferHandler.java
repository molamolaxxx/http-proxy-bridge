package com.mola.proxy.bridge.core.handlers.transfer;

import com.mola.proxy.bridge.core.entity.ProxyBridge;
import com.mola.proxy.bridge.core.registry.ProxyBridgeRegistry;
import com.mola.proxy.bridge.core.pool.ReverseProxyConnectPool;
import com.mola.proxy.bridge.core.utils.RemotingHelper;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author : molamola
 * @Project: http-proxy-bridge
 * @Description: 正向代理的流量转发到反向代理的channel
 * @date : 2023-09-30 09:10
 **/

@ChannelHandler.Sharable
public class DataTransferHandler extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DataTransferHandler.class);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ReverseProxyConnectPool connectPool = ReverseProxyConnectPool.instance();
        ProxyBridgeRegistry bridgeRegistry = ProxyBridgeRegistry.instance();

        ProxyBridge proxyBridge = bridgeRegistry.fetchBridgeByForwardPort(
                RemotingHelper.fetchChannelLocalPort(ctx.channel())
        );

        if (connectPool.getReverseProxyChannels(proxyBridge.getReversePort()).size() == 0) {
            ctx.fireChannelRead(msg);
            return;
        }
        Channel reverseChannel = connectPool.allocate(ctx.channel(), proxyBridge.getReversePort());
        if (Objects.isNull(reverseChannel)) {
            connectPool.clearChannels(proxyBridge.getReversePort());
            reverseChannel = connectPool.allocate(ctx.channel(), proxyBridge.getReversePort());
        }

        if (Objects.isNull(reverseChannel)) {
            log.debug("allocate reverseChannel failed, no available channel, forward = " + ctx.channel());
            ctx.fireChannelRead(msg);
            return;
        }

        reverseChannel.writeAndFlush(msg);
    }
}
